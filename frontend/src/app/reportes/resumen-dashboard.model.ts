/**
 * Respuesta de GET /api/reportes/resumen (ms-reportes). Alimenta el dashboard
 * de la pantalla de inicio en UNA sola petición: ms-reportes agrega los conteos
 * de ms-catalogo/ms-clientes/ms-identidad y el resumen de ventas de ms-ventas.
 */
export interface ResumenPeriodo {
  /** Cantidad de ventas del periodo. */
  cantidad: number;
  /** Suma de total_con_iva de esas ventas. */
  montoTotalConIva: number;
}

export interface ResumenVentas {
  hoy: ResumenPeriodo;
  mes: ResumenPeriodo;
}

export interface ResumenDashboard {
  productos: number;
  clientes: number;
  proveedores: number;
  /** Usuarios con activo = true, sin contar el usuario de sistema. */
  usuariosActivos: number;
  ventas: ResumenVentas;
}
