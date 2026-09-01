/** Una línea del formulario de venta: solo lo que escribe el cajero. */
export interface LineaVentaRequest {
  codigoProducto: number;
  cantidad: number;
}

/** POST /api/ventas. La cédula del usuario NO va acá: sale del JWT. */
export interface RegistrarVentaRequest {
  cedulaCliente: number;
  detalles: LineaVentaRequest[];
}

/** Una línea de la venta ya registrada, con los valores que calculó ms-ventas. */
export interface LineaVentaResponse {
  idDetalle: number;
  codigoProducto: number;
  nombreProducto: string;
  cantidad: number;
  valorUnitario: number;
  ivaPorcentaje: number;
  valorIva: number;
  valorTotal: number;
}

/**
 * Respuesta de una venta registrada. totalVenta = base gravable (sin IVA),
 * valorIva = IVA, totalConIva = totalVenta + valorIva.
 */
export interface VentaResponse {
  codigoVenta: number;
  cedulaCliente: number;
  cedulaUsuario: number;
  totalVenta: number;
  valorIva: number;
  totalConIva: number;
  fechaVenta: string;
  detalles: LineaVentaResponse[];
}
