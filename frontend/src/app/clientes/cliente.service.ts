import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { ClienteRequest, ClienteResponse, ClienteUpdateRequest } from './cliente.model';

/** CRUD de clientes (módulo 3) contra ms-clientes. */
@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.clientesUrl}/api/clientes`;

  listar() {
    return this.http.get<ClienteResponse[]>(this.base);
  }

  buscarPorCedula(cedula: number) {
    return this.http.get<ClienteResponse>(`${this.base}/${cedula}`);
  }

  crear(request: ClienteRequest) {
    return this.http.post<ClienteResponse>(this.base, request);
  }

  actualizar(cedula: number, request: ClienteUpdateRequest) {
    return this.http.put<ClienteResponse>(`${this.base}/${cedula}`, request);
  }

  eliminar(cedula: number) {
    return this.http.delete<void>(`${this.base}/${cedula}`);
  }
}
