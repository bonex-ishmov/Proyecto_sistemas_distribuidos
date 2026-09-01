import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UsuarioService } from '../usuario.service';

/**
 * Crear/editar usuario (módulo 2) en un solo componente: el modo lo decide la
 * ruta (`/usuarios/nuevo` vs `/usuarios/:cedula/editar`). En edición la cédula
 * es de solo lectura (es la PK) y la contraseña es opcional (vacío = se
 * conserva la actual); en creación ambas son obligatorias.
 */
@Component({
  selector: 'app-usuario-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './usuario-formulario.html',
  styleUrl: './usuario-formulario.css',
})
export class UsuarioFormulario implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usuarioService = inject(UsuarioService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  private readonly cedula = signal<number | null>(null);
  protected readonly modoEdicion = computed(() => this.cedula() !== null);

  protected readonly formulario = this.fb.nonNullable.group({
    cedulaUsuario: ['', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]],
    nombreUsuario: ['', [Validators.required, Validators.maxLength(100)]],
    emailUsuario: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    usuario: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.minLength(6)]],
  });

  ngOnInit(): void {
    const cedulaParam = this.route.snapshot.paramMap.get('cedula');

    if (cedulaParam === null) {
      // Modo creación: la contraseña sí es obligatoria acá.
      const passwordControl = this.formulario.controls.password;
      passwordControl.addValidators(Validators.required);
      passwordControl.updateValueAndValidity();
      return;
    }

    const cedula = Number(cedulaParam);
    this.cedula.set(cedula);
    this.formulario.controls.cedulaUsuario.disable();

    this.usuarioService.buscarPorCedula(cedula).subscribe((usuario) => {
      this.formulario.patchValue({
        cedulaUsuario: String(usuario.cedulaUsuario),
        nombreUsuario: usuario.nombreUsuario,
        emailUsuario: usuario.emailUsuario,
        usuario: usuario.usuario,
      });
    });
  }

  protected guardar(): void {
    if (this.formulario.invalid || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    const valores = this.formulario.getRawValue();

    const peticion = this.modoEdicion()
      ? this.usuarioService.actualizar(this.cedula()!, {
          nombreUsuario: valores.nombreUsuario,
          emailUsuario: valores.emailUsuario,
          usuario: valores.usuario,
          password: valores.password || undefined,
        })
      : this.usuarioService.crear({
          cedulaUsuario: Number(valores.cedulaUsuario),
          nombreUsuario: valores.nombreUsuario,
          emailUsuario: valores.emailUsuario,
          usuario: valores.usuario,
          password: valores.password,
        });

    peticion.subscribe({
      next: () => this.router.navigateByUrl('/usuarios'),
      // El banner global ya mostró el motivo (409 por cédula/usuario/correo
      // duplicado, 400 por validación, etc.); acá solo se reactiva el botón.
      error: () => this.enviando.set(false),
    });
  }
}
