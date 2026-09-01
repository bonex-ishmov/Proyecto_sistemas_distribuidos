import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ErrorValidacionResponse } from '../../core/error-response.model';
import { CargaResponse } from '../producto.model';
import { ProductoService } from '../producto.service';

/**
 * Carga masiva de productos por CSV (módulo 5). Reemplaza la tabla entera:
 * ver advertencia de "base compartida" en README.md — no correr mientras un
 * compañero está trabajando.
 *
 * El resumen de éxito y el mensaje genérico de error los cubre el patrón ya
 * establecido (banner global de error.interceptor para el mensaje corto),
 * pero el detalle línea-por-línea de un 422 (ErrorValidacionResponse.errores)
 * es información propia de esta pantalla: se captura acá para mostrarla
 * completa, no solo el resumen que cabe en el banner.
 */
@Component({
  selector: 'app-productos-carga',
  imports: [RouterLink],
  templateUrl: './productos-carga.html',
  styleUrl: './productos-carga.css',
})
export class ProductosCarga {
  private readonly productoService = inject(ProductoService);

  protected readonly archivo = signal<File | null>(null);
  protected readonly enviando = signal(false);
  protected readonly resultado = signal<CargaResponse | null>(null);
  protected readonly errores = signal<string[] | null>(null);

  protected seleccionar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.archivo.set(input.files?.[0] ?? null);
    this.resultado.set(null);
    this.errores.set(null);
  }

  protected cargar(): void {
    const archivo = this.archivo();
    if (!archivo || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.resultado.set(null);
    this.errores.set(null);

    this.productoService.cargar(archivo).subscribe({
      next: (respuesta) => {
        this.resultado.set(respuesta);
        this.enviando.set(false);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 422) {
          const body = error.error as ErrorValidacionResponse;
          this.errores.set(body.errores ?? []);
        }
        // El resto de errores (400 archivo inválido, 413 muy grande, 503,
        // etc.) ya los muestra el banner global con el mensaje del backend.
        this.enviando.set(false);
      },
    });
  }
}
