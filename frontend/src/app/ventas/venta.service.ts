import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { RegistrarVentaRequest, VentaResponse } from './venta.model';

/** Registro y consulta de ventas (módulo 6) contra ms-ventas. */
@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.ventasUrl}/api/ventas`;

  registrar(request: RegistrarVentaRequest) {
    return this.http.post<VentaResponse>(this.base, request);
  }

  buscarPorCodigo(codigo: number) {
    return this.http.get<VentaResponse>(`${this.base}/${codigo}`);
  }
}
