import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ReporteService } from '../reporte.service';
import { ClienteResponse } from '../../clientes/cliente.model';

/** Consulta b (módulo 7): listado de clientes. */
@Component({
  selector: 'app-reporte-clientes',
  imports: [RouterLink, Avatar],
  templateUrl: './reporte-clientes.html',
  styleUrl: '../reportes.css',
})
export class ReporteClientes implements OnInit {
  private readonly reporteService = inject(ReporteService);

  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.reporteService.listarClientes().subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
