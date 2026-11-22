# =====================================================================
# infra/oci/salidas.tf  --  datos que se imprimen tras `terraform apply`
# (y que leen nodos.sh y la etapa de despliegue con `terraform output`).
# =====================================================================

output "cluster_id" {
  value = oci_containerengine_cluster.tienda.id
}

output "node_pool_id" {
  value = oci_containerengine_node_pool.nodos.id
}

output "imagen_nodo" {
  description = "Imagen de Oracle Linux elegida para los nodos."
  value       = local.imagen_nodo.source_name
}

output "configurar_kubectl" {
  description = "Comando que deja a kubectl apuntando a este clúster (escribe ~/.kube/config)."
  value       = "oci ce cluster create-kubeconfig --cluster-id ${oci_containerengine_cluster.tienda.id} --file ~/.kube/config --region ${var.region} --token-version 2.0.0 --kube-endpoint PUBLIC_ENDPOINT"
}
