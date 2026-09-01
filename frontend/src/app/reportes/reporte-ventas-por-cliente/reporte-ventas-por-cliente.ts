import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ReporteService } from '../reporte.service';
import { VentasPorClienteResponse } from '../ventas-por-cliente.model';

/** Consulta c (módulo 7): total con IVA acumulado por cliente. */
@Component({
  selector: 'app-reporte-ventas-por-cliente',
  imports: [RouterLink, DecimalPipe, Avatar],
  templateUrl: './reporte-ventas-por-cliente.html',
  styleUrl: '../reportes.css',
})
export class ReporteVentasPorCliente implements OnInit {
  private readonly reporteService = inject(ReporteService);

  protected readonly filas = signal<VentasPorClienteResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.reporteService.ventasPorCliente().subscribe({
      next: (filas) => {
        this.filas.set(filas);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
