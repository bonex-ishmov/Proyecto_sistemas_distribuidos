import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SesionService } from './sesion.service';

/**
 * Adjunta el JWT guardado (localStorage, vía SesionService) como
 * "Authorization: Bearer ..." a toda petición saliente. Si no hay token
 * (todavía no se hizo login, o se acaba de limpiar) la petición sale igual,
 * sin el header — el backend la va a rechazar con 401 si la ruta lo exige,
 * y de eso se encarga error.interceptor.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(SesionService).obtenerToken();

  if (!token) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
