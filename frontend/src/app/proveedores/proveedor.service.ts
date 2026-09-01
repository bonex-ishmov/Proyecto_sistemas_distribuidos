import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { ProveedorRequest, ProveedorResponse, ProveedorUpdateRequest } from './proveedor.model';

/** CRUD de proveedores (módulo 4) contra ms-catalogo. */
@Injectable({ providedIn: 'root' })
export class ProveedorService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.catalogoUrl}/api/proveedores`;

  listar() {
    return this.http.get<ProveedorResponse[]>(this.base);
  }

  buscarPorNit(nit: number) {
    return this.http.get<ProveedorResponse>(`${this.base}/${nit}`);
  }

  crear(request: ProveedorRequest) {
    return this.http.post<ProveedorResponse>(this.base, request);
  }

  actualizar(nit: number, request: ProveedorUpdateRequest) {
    return this.http.put<ProveedorResponse>(`${this.base}/${nit}`, request);
  }

  eliminar(nit: number) {
    return this.http.delete<void>(`${this.base}/${nit}`);
  }
}
