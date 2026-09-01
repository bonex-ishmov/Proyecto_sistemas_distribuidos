/** GET /api/usuarios (y respuesta de crear/actualizar/cambiar estado). Nunca trae password ni hash. */
export interface UsuarioResponse {
  cedulaUsuario: number;
  nombreUsuario: string;
  emailUsuario: string;
  usuario: string;
  activo: boolean;
}

/** POST /api/usuarios. La cédula la manda el cliente: es la PK. */
export interface UsuarioRequest {
  cedulaUsuario: number;
  nombreUsuario: string;
  emailUsuario: string;
  usuario: string;
  password: string;
}

/** PUT /api/usuarios/{cedula}. Sin cédula (va en la URL); password opcional: vacío = se conserva. */
export interface UsuarioUpdateRequest {
  nombreUsuario: string;
  emailUsuario: string;
  usuario: string;
  password?: string;
}
