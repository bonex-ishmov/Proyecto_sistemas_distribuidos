import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { NotificacionService } from './core/notificacion.service';
import { SesionService } from './core/sesion.service';
import { Avatar } from './shared/avatar/avatar';

/**
 * Shell de la aplicación: sidebar oscuro fijo (nav + usuario/logout, solo con
 * sesión activa) + banner de error + área de ruteo. Las pantallas de negocio
 * se montan dentro de <router-outlet>.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Avatar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly notificacion = inject(NotificacionService);
  protected readonly sesion = inject(SesionService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected cerrarSesion(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
