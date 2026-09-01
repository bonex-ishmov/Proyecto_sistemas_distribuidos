import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionService } from './sesion.service';

/** Protege una ruta: sin JWT vigente, redirige a /login. */
export const authGuard: CanActivateFn = () => {
  if (inject(SesionService).haySesionActiva()) {
    return true;
  }
  return inject(Router).parseUrl('/login');
};
