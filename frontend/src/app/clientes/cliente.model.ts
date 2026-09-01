/** GET /api/clientes (y respuesta de crear/actualizar). */
export interface ClienteResponse {
  cedulaCliente: number;
  nombreCliente: string;
  direccionCliente: string;
  telefonoCliente: string;
  emailCliente: string;
}

/** POST /api/clientes. La cédula la manda el cliente: es la PK. */
export interface ClienteRequest {
  cedulaCliente: number;
  nombreCliente: string;
  direccionCliente: string;
  telefonoCliente: string;
  emailCliente: string;
}

/** PUT /api/clientes/{cedula}. Sin cédula: va en la URL. */
export interface ClienteUpdateRequest {
  nombreCliente: string;
  direccionCliente: string;
  telefonoCliente: string;
  emailCliente: string;
}
