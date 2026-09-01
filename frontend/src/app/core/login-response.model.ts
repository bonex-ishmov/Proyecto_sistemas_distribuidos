/** Respuesta de POST /api/auth/login en ms-identidad. Nunca trae password ni hash. */
export interface LoginResponse {
  token: string;
  tipo: string;
  cedulaUsuario: number;
  usuario: string;
  nombreUsuario: string;
  expiraEnMs: number;
}
