# =====================================================================
# infra/oci/presupuesto.tf  --  alerta de gasto por correo
#
# Un presupuesto en OCI NO bloquea nada: solo avisa. El freno real es que la
# cuenta sigue en Free Tier (no se puede gastar más que el crédito de prueba).
# Mientras dure la prueba, lo que se "gasta" sale del crédito de USD 300.
# =====================================================================

resource "oci_budget_budget" "mensual" {
  compartment_id = var.tenancy_ocid # los presupuestos viven en el compartimento raíz
  display_name   = "tienda-mensual"
  description    = "Gasto mensual del proyecto Tienda Generica"
  amount         = var.presupuesto_usd
  reset_period   = "MONTHLY"
  target_type    = "COMPARTMENT"
  targets        = [var.tenancy_ocid]
}

# Aviso al gastar la mitad del presupuesto del mes.
resource "oci_budget_alert_rule" "mitad" {
  budget_id      = oci_budget_budget.mensual.id
  display_name   = "tienda-50-por-ciento"
  type           = "ACTUAL"
  threshold      = 50
  threshold_type = "PERCENTAGE"
  recipients     = var.correo_alertas
  message        = "Tienda Generica: ya se gasto el 50 % del presupuesto del mes en Oracle Cloud. Revisa si los nodos quedaron encendidos (infra/oci/nodos.sh estado)."
}

# Aviso al gastarlo entero.
resource "oci_budget_alert_rule" "completo" {
  budget_id      = oci_budget_budget.mensual.id
  display_name   = "tienda-100-por-ciento"
  type           = "ACTUAL"
  threshold      = 100
  threshold_type = "PERCENTAGE"
  recipients     = var.correo_alertas
  message        = "Tienda Generica: se gasto el 100 % del presupuesto del mes en Oracle Cloud. Apaga los nodos: infra/oci/nodos.sh apagar"
}

# Aviso temprano: si al ritmo actual el mes terminaría por encima del presupuesto.
resource "oci_budget_alert_rule" "pronostico" {
  budget_id      = oci_budget_budget.mensual.id
  display_name   = "tienda-pronostico"
  type           = "FORECAST"
  threshold      = 100
  threshold_type = "PERCENTAGE"
  recipients     = var.correo_alertas
  message        = "Tienda Generica: al ritmo actual el gasto del mes superaria el presupuesto en Oracle Cloud."
}
