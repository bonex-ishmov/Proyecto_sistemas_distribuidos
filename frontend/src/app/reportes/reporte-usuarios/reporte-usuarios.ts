import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ReporteService } from '../reporte.service';
import { UsuarioResponse } from '../../usuarios/usuario.model';

/** Consulta a (módulo 7): listado de usuarios, sin el usuario de sistema. */
@Component({
  selector: 'app-reporte-usuarios',
  imports: [RouterLink, Avatar],
  templateUrl: './reporte-usuarios.html',
  styleUrl: '../reportes.css',
})
export class ReporteUsuarios implements OnInit {
  private readonly reporteService = inject(ReporteService);

  protected readonly usuarios = signal<UsuarioResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.reporteService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
