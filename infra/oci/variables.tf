# =====================================================================
# infra/oci/variables.tf  --  parámetros de la infraestructura
# Los valores propios de cada cuenta (tenancy, IP, correo) van en
# terraform.tfvars, que NO se versiona (ver terraform.tfvars.example).
# =====================================================================

variable "tenancy_ocid" {
  description = "OCID del tenancy. Todo se crea en el compartimento raíz (la cuenta es solo para este proyecto)."
  type        = string
}

variable "region" {
  description = "Región principal de la cuenta. En Free Tier, lo gratuito solo existe en esta región."
  type        = string
  default     = "us-sanjose-1"
}

variable "mi_ip" {
  description = "IP pública desde la que se administra el clúster (kubectl). Solo esa IP llega a la API de Kubernetes."
  type        = string
}

variable "correo_alertas" {
  description = "Correo que recibe las alertas de presupuesto."
  type        = string
}

variable "presupuesto_usd" {
  description = "Presupuesto mensual en USD. Se avisa al gastar el 50 % y el 100 %, y si el pronóstico del mes lo supera."
  type        = number
  default     = 50
}

variable "version_kubernetes" {
  description = "Versión de Kubernetes del clúster y de los nodos (de las que ofrece OKE: oci ce cluster-options get --cluster-option-id all)."
  type        = string
  default     = "v1.37.1"
}

# ---- Nodos (máquinas virtuales donde corren los pods) ----
# Hoy: E5.Flex (x86), pagado con el crédito de prueba, porque Oracle no tiene
# capacidad A1 en us-sanjose-1. Para pasar a A1.Flex (ARM, Always Free) basta con
# cambiar forma_nodo y memoria_nodo: la imagen del nodo se elige sola según la
# arquitectura, y las imágenes de la tienda en GHCR ya existen para ARM y x86.
variable "forma_nodo" {
  description = "Forma (tipo de máquina) de los nodos: VM.Standard.E5.Flex (x86) o VM.Standard.A1.Flex (ARM)."
  type        = string
  default     = "VM.Standard.E5.Flex"
}

variable "ocpus_nodo" {
  description = "OCPU por nodo. En x86, 1 OCPU = 1 núcleo físico = 2 vCPU."
  type        = number
  default     = 1
}

variable "memoria_nodo" {
  description = "GB de RAM por nodo. La tienda entera usa ~3,7 GB en reposo (medido en Docker); 2 x 8 GB deja holgura."
  type        = number
  default     = 8
}

variable "nodos_iniciales" {
  description = "Nodos al crear el grupo. Después el número lo cambia nodos.sh (encender/apagar) y Terraform lo ignora."
  type        = number
  default     = 2
}
