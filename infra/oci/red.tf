# =====================================================================
# infra/oci/red.tf  --  red virtual (VCN) del clúster
#
#   VCN 10.0.0.0/16
#   ├── api      10.0.0.0/28   pública  -> API de Kubernetes (kubectl). Solo var.mi_ip.
#   ├── nodos    10.0.10.0/24  PRIVADA  -> las máquinas de los nodos (sin IP pública).
#   │                                      Salen a internet por el NAT (bajar imágenes de GHCR).
#   └── balanceo 10.0.20.0/24  pública  -> el balanceador que Kubernetes crea para el
#                                          front (Service tipo LoadBalancer). Solo puerto 80.
#
# Pods y Services NO usan direcciones de la VCN: el clúster usa la red superpuesta
# flannel (pods 10.244.0.0/16, Services 10.96.0.0/16, ver oke.tf).
#
# Las puertas de enlace (internet, NAT, servicios) no tienen costo en OCI.
# =====================================================================

locals {
  cidr_vcn      = "10.0.0.0/16"
  cidr_api      = "10.0.0.0/28"
  cidr_nodos    = "10.0.10.0/24"
  cidr_balanceo = "10.0.20.0/24"
  todo_internet = "0.0.0.0/0"
}

resource "oci_core_vcn" "tienda" {
  compartment_id = var.tenancy_ocid
  display_name   = "tienda-vcn"
  cidr_blocks    = [local.cidr_vcn]
  dns_label      = "tienda"
}

# ---------------- Puertas de enlace ----------------

# Entrada/salida directa a internet, para las subredes públicas.
resource "oci_core_internet_gateway" "internet" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-internet"
}

# Solo salida a internet, para la subred privada de los nodos.
resource "oci_core_nat_gateway" "nat" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-nat"
}

# Camino interno hacia los servicios de Oracle (el nodo se registra en OKE por aquí).
data "oci_core_services" "oracle" {
  filter {
    name   = "name"
    values = ["All .* Services In Oracle Services Network"]
    regex  = true
  }
}

resource "oci_core_service_gateway" "servicios" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-servicios-oracle"
  services {
    service_id = data.oci_core_services.oracle.services[0].id
  }
}

# ---------------- Tablas de rutas ----------------

resource "oci_core_route_table" "publica" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-rutas-publicas"
  route_rules {
    destination       = local.todo_internet
    network_entity_id = oci_core_internet_gateway.internet.id
  }
}

resource "oci_core_route_table" "privada" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-rutas-privadas"
  route_rules {
    destination       = local.todo_internet
    network_entity_id = oci_core_nat_gateway.nat.id
  }
  route_rules {
    destination       = data.oci_core_services.oracle.services[0].cidr_block
    destination_type  = "SERVICE_CIDR_BLOCK"
    network_entity_id = oci_core_service_gateway.servicios.id
  }
}

# ---------------- Listas de seguridad (el firewall de cada subred) ----------------
# Son "stateful": si una conexión entra permitida, su respuesta sale sola (y al revés).

resource "oci_core_security_list" "api" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-sl-api"

  # kubectl desde tu IP, y nada más desde internet.
  ingress_security_rules {
    description = "kubectl desde la IP de administracion"
    protocol    = "6" # TCP
    source      = "${var.mi_ip}/32"
    tcp_options {
      min = 6443
      max = 6443
    }
  }
  # Los nodos hablan con el plano de control (6443 API, 12250 agente de OKE).
  ingress_security_rules {
    description = "nodos -> API de Kubernetes"
    protocol    = "6"
    source      = local.cidr_nodos
    tcp_options {
      min = 6443
      max = 6443
    }
  }
  ingress_security_rules {
    description = "nodos -> agente del plano de control"
    protocol    = "6"
    source      = local.cidr_nodos
    tcp_options {
      min = 12250
      max = 12250
    }
  }
  ingress_security_rules {
    description = "descubrimiento de MTU desde la VCN"
    protocol    = "1" # ICMP
    source      = local.cidr_vcn
    icmp_options {
      type = 3
      code = 4
    }
  }
  egress_security_rules {
    description = "plano de control -> nodos y servicios de Oracle"
    protocol    = "all"
    destination = local.todo_internet
  }
}

resource "oci_core_security_list" "nodos" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-sl-nodos"

  # Todo lo que venga de dentro de la VCN: otros nodos (pod a pod), el plano de
  # control y el balanceador (que llega a los NodePort 30000-32767 del front).
  ingress_security_rules {
    description = "trafico interno de la VCN"
    protocol    = "all"
    source      = local.cidr_vcn
  }
  ingress_security_rules {
    description = "descubrimiento de MTU"
    protocol    = "1"
    source      = local.todo_internet
    icmp_options {
      type = 3
      code = 4
    }
  }
  # Salida libre: bajar imágenes de ghcr.io y paquetes, por el NAT.
  egress_security_rules {
    description = "salida a internet por el NAT"
    protocol    = "all"
    destination = local.todo_internet
  }
}

resource "oci_core_security_list" "balanceo" {
  compartment_id = var.tenancy_ocid
  vcn_id         = oci_core_vcn.tienda.id
  display_name   = "tienda-sl-balanceo"

  # Lo único abierto al mundo: HTTP a la tienda.
  ingress_security_rules {
    description = "HTTP de los navegadores"
    protocol    = "6"
    source      = local.todo_internet
    tcp_options {
      min = 80
      max = 80
    }
  }
  egress_security_rules {
    description = "balanceador -> nodos (NodePort y chequeo de salud)"
    protocol    = "6"
    destination = local.cidr_nodos
  }
}

# ---------------- Subredes ----------------

resource "oci_core_subnet" "api" {
  compartment_id    = var.tenancy_ocid
  vcn_id            = oci_core_vcn.tienda.id
  display_name      = "tienda-api"
  cidr_block        = local.cidr_api
  dns_label         = "api"
  route_table_id    = oci_core_route_table.publica.id
  security_list_ids = [oci_core_security_list.api.id]
}

resource "oci_core_subnet" "nodos" {
  compartment_id             = var.tenancy_ocid
  vcn_id                     = oci_core_vcn.tienda.id
  display_name               = "tienda-nodos"
  cidr_block                 = local.cidr_nodos
  dns_label                  = "nodos"
  prohibit_public_ip_on_vnic = true # privada: ningún nodo recibe IP pública
  route_table_id             = oci_core_route_table.privada.id
  security_list_ids          = [oci_core_security_list.nodos.id]
}

resource "oci_core_subnet" "balanceo" {
  compartment_id    = var.tenancy_ocid
  vcn_id            = oci_core_vcn.tienda.id
  display_name      = "tienda-balanceo"
  cidr_block        = local.cidr_balanceo
  dns_label         = "balanceo"
  route_table_id    = oci_core_route_table.publica.id
  security_list_ids = [oci_core_security_list.balanceo.id]
}
