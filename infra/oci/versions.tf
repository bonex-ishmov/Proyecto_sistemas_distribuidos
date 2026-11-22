# =====================================================================
# infra/oci/versions.tf  --  versión de Terraform y del proveedor de Oracle
#
# Esta carpeta crea la INFRAESTRUCTURA en Oracle Cloud (red, clúster de
# Kubernetes OKE, nodos y alerta de presupuesto). La APLICACIÓN (los pods de
# la tienda) NO se despliega aquí: va en k8s/ con kubectl.
#
# Autenticación: el proveedor lee ~/.oci/config (perfil DEFAULT), el mismo
# archivo que usa el CLI `oci`. Ninguna credencial vive en esta carpeta.
# =====================================================================
terraform {
  required_version = ">= 1.9"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 9.14"
    }
  }
}

provider "oci" {
  region = var.region
}
