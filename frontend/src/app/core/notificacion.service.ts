import { Injectable, signal } from '@angular/core';

/**
 * Único punto donde se guarda el último mensaje de error para mostrarlo en el
 * banner del shell (app.html). Lo llena error.interceptor; lo lee y lo limpia
 * el shell. Sin cola de mensajes ni auto-dismiss: para el alcance actual (un
 * error a la vez, cerrado a mano) alcanza.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly _mensaje = signal<string | null>(null);
  readonly mensaje = this._mensaje.asReadonly();

  mostrar(mensaje: string): void {
    this._mensaje.set(mensaje);
  }

  limpiar(): void {
    this._mensaje.set(null);
  }
}
