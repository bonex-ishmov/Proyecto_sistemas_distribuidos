import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { UsuarioRequest, UsuarioResponse, UsuarioUpdateRequest } from './usuario.model';

/** CRUD de usuarios (módulo 2) contra ms-identidad. */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.identidadUrl}/api/usuarios`;

  listar() {
    return this.http.get<UsuarioResponse[]>(this.base);
  }

  buscarPorCedula(cedula: number) {
    return this.http.get<UsuarioResponse>(`${this.base}/${cedula}`);
  }

  crear(request: UsuarioRequest) {
    return this.http.post<UsuarioResponse>(this.base, request);
  }

  actualizar(cedula: number, request: UsuarioUpdateRequest) {
    return this.http.put<UsuarioResponse>(`${this.base}/${cedula}`, request);
  }

  eliminar(cedula: number) {
    return this.http.delete<void>(`${this.base}/${cedula}`);
  }

  cambiarEstado(cedula: number, activo: boolean) {
    return this.http.patch<UsuarioResponse>(`${this.base}/${cedula}/estado`, { activo });
  }
}
