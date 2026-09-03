import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { ClienteResponse } from '../clientes/cliente.model';
import { UsuarioResponse } from '../usuarios/usuario.model';
import { ResumenDashboard } from './resumen-dashboard.model';
import { VentasPorClienteResponse } from './ventas-por-cliente.model';

/**
 * Las 3 consultas del módulo 7 contra ms-reportes. Reutiliza los modelos de
 * ClienteResponse/UsuarioResponse: son exactamente los mismos DTOs que ya usan
 * las pantallas de usuarios/clientes (ms-reportes hace passthrough de ellos).
 */
@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.reportesUrl}/api/reportes`;

  listarUsuarios() {
    return this.http.get<UsuarioResponse[]>(`${this.base}/usuarios`);
  }

  listarClientes() {
    return this.http.get<ClienteResponse[]>(`${this.base}/clientes`);
  }

  ventasPorCliente() {
    return this.http.get<VentasPorClienteResponse[]>(`${this.base}/ventas-por-cliente`);
  }

  /** Resumen del dashboard de inicio: conteos + ventas de hoy y del mes. */
  resumenDashboard() {
    return this.http.get<ResumenDashboard>(`${this.base}/resumen`);
  }
}
