import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SesionService } from '../core/sesion.service';
import { ReporteService } from '../reportes/reporte.service';
import { ResumenDashboard } from '../reportes/resumen-dashboard.model';

/**
 * Pantalla de inicio: dashboard con métricas reales. Hace UNA petición a
 * ms-reportes (GET /api/reportes/resumen), que agrega los conteos de
 * catálogo/clientes/identidad y el resumen de ventas de ms-ventas.
 *
 * Zoneless: todo el estado que lee la plantilla vive en signals; el observable
 * de HttpClient lo cubre el scheduler zoneless (ver CLAUDE.md > Frontend).
 */
@Component({
  selector: 'app-home',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  protected readonly sesion = inject(SesionService);
  private readonly reporteService = inject(ReporteService);

  protected readonly resumen = signal<ResumenDashboard | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.reporteService.resumenDashboard().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.cargando.set(false);
      },
      // El banner global (errorInterceptor) ya muestra el detalle; acá solo se
      // deja el área del dashboard en un estado legible.
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
