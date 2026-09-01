/** Una fila de GET /api/reportes/ventas-por-cliente (consulta c, módulo 7). */
export interface VentasPorClienteResponse {
  cedulaCliente: number;
  nombreCliente: string;
  totalConIva: number;
}
