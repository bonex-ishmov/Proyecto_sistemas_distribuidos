import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { SIN_BANNER_GLOBAL } from '../core/error.interceptor';
import { CargaResponse, ProductoResponse } from './producto.model';

/**
 * Productos (módulo 5) contra ms-catalogo. Sin crear/editar/eliminar
 * individual: el backend solo expone lectura y carga masiva por CSV
 * (reemplazo total de la tabla, ver CLAUDE.md).
 */
@Injectable({ providedIn: 'root' })
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.catalogoUrl}/api/productos`;

  listar() {
    return this.http.get<ProductoResponse[]>(this.base);
  }

  /**
   * Usado por la previsualización línea a línea del formulario de venta: un
   * 404 acá es un resultado normal ("no existe ese código"), no un error de
   * aplicación, así que va con SIN_BANNER_GLOBAL — lo muestra la línea, no
   * el banner de arriba.
   */
  buscarPorCodigo(codigo: number) {
    return this.http.get<ProductoResponse>(`${this.base}/${codigo}`, {
      context: new HttpContext().set(SIN_BANNER_GLOBAL, true),
    });
  }

  cargar(archivo: File) {
    const formData = new FormData();
    formData.append('archivo', archivo);
    // Sin Content-Type manual: el navegador arma el multipart/form-data con
    // el boundary correcto solo si HttpClient no lo fija a mano.
    return this.http.post<CargaResponse>(`${this.base}/carga`, formData);
  }
}
