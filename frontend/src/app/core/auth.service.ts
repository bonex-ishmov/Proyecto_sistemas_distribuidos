import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse } from './login-response.model';
import { SesionService } from './sesion.service';

/** Login/logout contra ms-identidad (único servicio que emite JWT). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(SesionService);

  login(usuario: string, password: string) {
    return this.http
      .post<LoginResponse>(`${environment.identidadUrl}/api/auth/login`, { usuario, password })
      .pipe(tap((respuesta) => this.sesion.guardarToken(respuesta.token)));
  }

  /** No hay endpoint de logout: el backend es stateless, no hay nada que invalidar ahí. */
  logout(): void {
    this.sesion.limpiarToken();
  }
}
