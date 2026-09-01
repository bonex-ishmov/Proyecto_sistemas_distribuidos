import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { UsuarioService } from '../usuario.service';
import { UsuarioResponse } from '../usuario.model';

/** Listado de usuarios (módulo 2, consulta principal). */
@Component({
  selector: 'app-usuarios-lista',
  imports: [RouterLink, Avatar],
  templateUrl: './usuarios-lista.html',
  styleUrl: './usuarios-lista.css',
})
export class UsuariosLista implements OnInit {
  private readonly usuarioService = inject(UsuarioService);

  protected readonly usuarios = signal<UsuarioResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarEstado(usuario: UsuarioResponse): void {
    this.usuarioService.cambiarEstado(usuario.cedulaUsuario, !usuario.activo).subscribe({
      next: (actualizado) => this.reemplazar(actualizado),
    });
  }

  protected eliminar(usuario: UsuarioResponse): void {
    if (!confirm(`¿Eliminar al usuario '${usuario.usuario}'? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.usuarioService.eliminar(usuario.cedulaUsuario).subscribe({
      next: () =>
        this.usuarios.update((lista) =>
          lista.filter((u) => u.cedulaUsuario !== usuario.cedulaUsuario),
        ),
      // Si es el usuario inicial, ms-identidad responde 409 y el banner global
      // ya muestra "El usuario inicial no se puede eliminar, solo desactivar".
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.usuarioService.listar().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  private reemplazar(actualizado: UsuarioResponse): void {
    this.usuarios.update((lista) =>
      lista.map((u) => (u.cedulaUsuario === actualizado.cedulaUsuario ? actualizado : u)),
    );
  }
}
