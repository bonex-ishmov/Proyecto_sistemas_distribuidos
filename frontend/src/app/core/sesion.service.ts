import { Injectable, signal } from '@angular/core';

const CLAVE_TOKEN = 'tienda.token';

/**
 * Claims del JWT que emite ms-identidad (JwtService.generarToken): subject =
 * usuario, claims = cedula y nombre. Ver "Authentication" en CLAUDE.md.
 */
interface ClaimsJwt {
  sub: string;
  cedula: number;
  nombre: string;
  iat: number;
  exp: number;
}

/**
 * Único punto de acceso al JWT guardado en localStorage (decisión de
 * arquitectura: ver "Autenticación" en CLAUDE.md — localStorage, sin refresh
 * token, expira a la hora). No hay estado de sesión en el backend: "haber
 * iniciado sesión" es, únicamente, "tener acá un JWT vigente".
 *
 * El nombre del usuario logueado se lee del propio JWT (claim `nombre`), no
 * se guarda aparte: así no hay dos copias que puedan desincronizarse.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly _nombreUsuario = signal<string | null>(this.leerClaims()?.nombre ?? null);
  readonly nombreUsuario = this._nombreUsuario.asReadonly();

  guardarToken(token: string): void {
    localStorage.setItem(CLAVE_TOKEN, token);
    this._nombreUsuario.set(this.leerClaims()?.nombre ?? null);
  }

  obtenerToken(): string | null {
    return localStorage.getItem(CLAVE_TOKEN);
  }

  limpiarToken(): void {
    localStorage.removeItem(CLAVE_TOKEN);
    this._nombreUsuario.set(null);
  }

  /** true solo si hay un token guardado y su claim `exp` todavía no pasó. */
  haySesionActiva(): boolean {
    const claims = this.leerClaims();
    return claims !== null && Date.now() < claims.exp * 1000;
  }

  private leerClaims(): ClaimsJwt | null {
    const token = this.obtenerToken();
    if (!token) {
      return null;
    }
    try {
      const payloadBase64Url = token.split('.')[1];
      const payloadJson = atob(payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(payloadJson) as ClaimsJwt;
    } catch {
      // Token corrupto o no-JWT: se trata igual que "no hay sesión".
      return null;
    }
  }
}
