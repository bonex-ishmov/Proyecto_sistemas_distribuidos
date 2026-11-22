# =====================================================================
# infra/oci/oke.tf  --  clúster de Kubernetes (OKE) y sus nodos
# =====================================================================

data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

# Imágenes de nodo que ofrece OKE (Oracle Linux con Kubernetes ya instalado).
data "oci_containerengine_node_pool_option" "opciones" {
  node_pool_option_id = "all"
  compartment_id      = var.tenancy_ocid
}

locals {
  # us-sanjose-1 tiene un solo dominio de disponibilidad.
  dominio = data.oci_identity_availability_domains.ads.availability_domains[0].name

  # Las formas A1 son ARM (aarch64); el resto, x86.
  es_arm = length(regexall("A1", var.forma_nodo)) > 0

  # Imagen de nodo: Oracle Linux 9, de la versión de Kubernetes elegida, de la
  # arquitectura de la forma, sin GPU. Oracle las lista de la más nueva a la más
  # vieja, así que [0] es la última.
  imagenes = [
    for s in data.oci_containerengine_node_pool_option.opciones.sources : s
    if length(regexall("^Oracle-Linux-9", s.source_name)) > 0
    && length(regexall("-OKE-${trimprefix(var.version_kubernetes, "v")}-", s.source_name)) > 0
    && length(regexall("GPU", s.source_name)) == 0
    && local.es_arm == (length(regexall("aarch64", s.source_name)) > 0)
  ]
  imagen_nodo = local.imagenes[0]
}

resource "oci_containerengine_cluster" "tienda" {
  compartment_id     = var.tenancy_ocid
  name               = "tienda"
  vcn_id             = oci_core_vcn.tienda.id
  kubernetes_version = var.version_kubernetes
  # BASIC_CLUSTER: el plano de control no se cobra. (ENHANCED cuesta por hora.)
  type = "BASIC_CLUSTER"

  # flannel: los pods usan una red superpuesta propia, sin gastar IPs de la VCN.
  cluster_pod_network_options {
    cni_type = "FLANNEL_OVERLAY"
  }

  endpoint_config {
    subnet_id            = oci_core_subnet.api.id
    is_public_ip_enabled = true # kubectl desde tu casa; la lista de seguridad deja pasar solo var.mi_ip
  }

  options {
    # Aquí pone Kubernetes los balanceadores de los Service tipo LoadBalancer.
    service_lb_subnet_ids = [oci_core_subnet.balanceo.id]
    kubernetes_network_config {
      pods_cidr     = "10.244.0.0/16"
      services_cidr = "10.96.0.0/16"
    }
  }
}

resource "oci_containerengine_node_pool" "nodos" {
  compartment_id     = var.tenancy_ocid
  cluster_id         = oci_containerengine_cluster.tienda.id
  name               = "tienda-nodos"
  kubernetes_version = var.version_kubernetes

  node_shape = var.forma_nodo
  node_shape_config {
    ocpus         = var.ocpus_nodo
    memory_in_gbs = var.memoria_nodo
  }

  node_source_details {
    source_type             = "IMAGE"
    image_id                = local.imagen_nodo.image_id
    boot_volume_size_in_gbs = 50 # el mínimo; 2 nodos x 50 GB entran en los 200 GB gratuitos
  }

  node_config_details {
    size = var.nodos_iniciales
    placement_configs {
      availability_domain = local.dominio
      subnet_id           = oci_core_subnet.nodos.id
    }
  }

  # El número de nodos lo maneja nodos.sh (encender = 2, apagar = 0). Sin esto,
  # el siguiente `terraform apply` lo devolvería a nodos_iniciales.
  lifecycle {
    ignore_changes = [node_config_details[0].size]
  }
}
