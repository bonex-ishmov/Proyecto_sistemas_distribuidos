import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ErrorResponse } from './error-response.model';
import { NotificacionService } from './notificacion.service';
import { SesionService } from './sesion.service';

/**
 * Poné este contexto en `true` en una petición para que el banner global NO
 * muestre su error — lo usa, por ejemplo, la búsqueda de producto línea por
 * línea del formulario de venta: un 404 ahí es "no existe ese código", un
 * mensaje que pertenece a esa línea, no un error de la aplicación entera.
 * El manejo de 401 (sesión expirada) NO se salta con este flag: es
 * independiente de qué pantalla hizo la petición.
 */
export const SIN_BANNER_GLOBAL = new HttpContextToken<boolean>(() => false);

/**
 * Traduce cualquier error HTTP de los 5 microservicios a un mensaje visible en
 * el banner del shell (NotificacionService), y deja pasar el error tal cual
 * para que quien hizo la petición pueda seguir reaccionando (p.ej. reactivar
 * un botón de guardar, o mostrar su propio mensaje si pidió SIN_BANNER_GLOBAL).
 *
 * El body de error, cuando el microservicio alcanzó a responder, sigue el
 * ErrorResponse uniforme de GlobalExceptionHandler.
 *
 * 401 es caso especial, PERO SOLO fuera de POST /api/auth/login: en cualquier
 * otra ruta un 401 significa "el JWT que mandamos no sirve" (vencido, o no
 * hay), así que se limpia el token y se manda a /login con un mensaje de
 * sesión expirada. Un 401 DEL login (usuario/contraseña incorrectos, o
 * usuario inactivo) es un resultado normal del formulario, no una sesión que
 * expiró: ese sigue el camino genérico de abajo y muestra el mensaje real que
 * mandó ms-identidad.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificacion = inject(NotificacionService);
  const sesion = inject(SesionService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        if (!req.context.get(SIN_BANNER_GLOBAL)) {
          notificacion.mostrar('Ocurrió un error inesperado.');
        }
        return throwError(() => error);
      }

      if (error.status === 401 && !esPeticionDeLogin(req)) {
        sesion.limpiarToken();
        notificacion.mostrar('Tu sesión expiró. Iniciá sesión de nuevo.');
        if (!router.url.startsWith('/login')) {
          router.navigateByUrl('/login');
        }
      } else if (!req.context.get(SIN_BANNER_GLOBAL)) {
        notificacion.mostrar(mensajeParaError(error));
      }

      return throwError(() => error);
    }),
  );
};

function esPeticionDeLogin(req: HttpRequest<unknown>): boolean {
  return req.url.endsWith('/api/auth/login');
}

/**
 * Exportada: la reutiliza quien necesite el mismo mensaje "legible" de un
 * error fuera del banner global (ver productos-carga y venta-formulario).
 */
export function mensajeParaError(error: HttpErrorResponse): string {
  if (error.status === 0) {
    // Sin respuesta: servicio caído, URL mal configurada, o CORS bloqueó la
    // petición (en ese caso el navegador tampoco deja leer el body del error).
    return 'No se pudo conectar con el servidor. Verificá que el microservicio esté corriendo.';
  }

  const body = error.error as ErrorResponse | null;
  if (body && typeof body.mensaje === 'string') {
    return body.mensaje;
  }

  return `Error inesperado del servidor (${error.status}).`;
}
