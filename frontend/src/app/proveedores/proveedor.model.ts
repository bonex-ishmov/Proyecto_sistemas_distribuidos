/** GET /api/proveedores (y respuesta de crear/actualizar). */
export interface ProveedorResponse {
  nitproveedor: number;
  nombreProveedor: string;
  direccionProveedor: string;
  telefonoProveedor: string;
  ciudadProveedor: string;
}

/** POST /api/proveedores. El NIT lo manda el cliente: es la PK. */
export interface ProveedorRequest {
  nitproveedor: number;
  nombreProveedor: string;
  direccionProveedor: string;
  telefonoProveedor: string;
  ciudadProveedor: string;
}

/** PUT /api/proveedores/{nit}. Sin NIT: va en la URL. */
export interface ProveedorUpdateRequest {
  nombreProveedor: string;
  direccionProveedor: string;
  telefonoProveedor: string;
  ciudadProveedor: string;
}
