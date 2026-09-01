import { Component, inject } from '@angular/core';
import { SesionService } from '../core/sesion.service';

/**
 * Placeholder de inicio (ruta protegida por authGuard, ver app.routes.ts).
 * Nav y logout viven en el shell (app.html) desde que hay más de una pantalla
 * protegida. Sin contenido de negocio propio: los módulos 3-7 llegan en los
 * incrementos siguientes.
 */
@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  protected readonly sesion = inject(SesionService);
}
