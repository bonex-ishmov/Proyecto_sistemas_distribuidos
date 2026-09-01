import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

/**
 * Pantalla de login (módulo 1). El error de credenciales inválidas NO se
 * maneja acá: lo muestra el banner global (error.interceptor lee el mensaje
 * real que manda ms-identidad — "Usuario o contraseña errados..." o "El
 * usuario se encuentra inactivo").
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);

  protected readonly formulario = this.fb.nonNullable.group({
    usuario: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected iniciarSesion(): void {
    if (this.formulario.invalid || this.enviando()) {
      return;
    }

    const { usuario, password } = this.formulario.getRawValue();
    this.enviando.set(true);

    this.authService.login(usuario, password).subscribe({
      next: () => this.router.navigateByUrl('/'),
      // El banner global (error.interceptor) ya mostró el mensaje; acá solo
      // hay que reactivar el formulario para que se pueda reintentar.
      error: () => this.enviando.set(false),
    });
  }
}
