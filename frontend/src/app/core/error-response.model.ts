/**
 * Forma única de error que devuelven los 5 microservicios: el ErrorResponse
 * que arma GlobalExceptionHandler en cada uno (mismos 5 campos, mismo orden).
 */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  path: string;
}

/**
 * Variante que usa ms-catalogo para la carga CSV de productos (422): mismos
 * campos de ErrorResponse más el detalle línea por línea de lo que falló.
 */
export interface ErrorValidacionResponse extends ErrorResponse {
  errores: string[];
}
