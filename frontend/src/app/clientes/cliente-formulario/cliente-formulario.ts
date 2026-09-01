import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClienteService } from '../cliente.service';

/**
 * Crear/editar cliente (módulo 3), mismo patrón que usuario-formulario: el
 * modo lo decide la ruta (`/clientes/nuevo` vs `/clientes/:cedula/editar`).
 * En edición la cédula es de solo lectura (es la PK).
 */
@Component({
  selector: 'app-cliente-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cliente-formulario.html',
  styleUrl: './cliente-formulario.css',
})
export class ClienteFormulario implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clienteService = inject(ClienteService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  private readonly cedula = signal<number | null>(null);
  protected readonly modoEdicion = computed(() => this.cedula() !== null);

  protected readonly formulario = this.fb.nonNullable.group({
    cedulaCliente: ['', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]],
    nombreCliente: ['', [Validators.required, Validators.maxLength(100)]],
    direccionCliente: ['', [Validators.required, Validators.maxLength(150)]],
    telefonoCliente: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]{7,20}$/)]],
    emailCliente: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    const cedulaParam = this.route.snapshot.paramMap.get('cedula');
    if (cedulaParam === null) {
      return;
    }

    const cedula = Number(cedulaParam);
    this.cedula.set(cedula);
    this.formulario.controls.cedulaCliente.disable();

    this.clienteService.buscarPorCedula(cedula).subscribe((cliente) => {
      this.formulario.patchValue({
        cedulaCliente: String(cliente.cedulaCliente),
        nombreCliente: cliente.nombreCliente,
        direccionCliente: cliente.direccionCliente,
        telefonoCliente: cliente.telefonoCliente,
        emailCliente: cliente.emailCliente,
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
      ? this.clienteService.actualizar(this.cedula()!, {
          nombreCliente: valores.nombreCliente,
          direccionCliente: valores.direccionCliente,
          telefonoCliente: valores.telefonoCliente,
          emailCliente: valores.emailCliente,
        })
      : this.clienteService.crear({
          cedulaCliente: Number(valores.cedulaCliente),
          nombreCliente: valores.nombreCliente,
          direccionCliente: valores.direccionCliente,
          telefonoCliente: valores.telefonoCliente,
          emailCliente: valores.emailCliente,
        });

    peticion.subscribe({
      next: () => this.router.navigateByUrl('/clientes'),
      error: () => this.enviando.set(false),
    });
  }
}
