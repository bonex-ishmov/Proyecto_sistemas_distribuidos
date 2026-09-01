/** GET /api/productos. Incluye nombreProveedor: se lo resuelve el backend, ahorra una llamada. */
export interface ProductoResponse {
  codigoProducto: number;
  nombreProducto: string;
  nitproveedor: number;
  nombreProveedor: string;
  precioCompra: number;
  ivacompra: number;
  precioVenta: number;
}

/** Respuesta de una carga CSV exitosa (POST /api/productos/carga). */
export interface CargaResponse {
  mensaje: string;
  registrosEliminados: number;
  registrosCargados: number;
}
