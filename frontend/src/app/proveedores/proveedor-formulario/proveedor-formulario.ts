import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProveedorService } from '../proveedor.service';

/**
 * Crear/editar proveedor (módulo 4), mismo patrón que cliente-formulario: el
 * modo lo decide la ruta. En edición el NIT es de solo lectura (es la PK).
 */
@Component({
  selector: 'app-proveedor-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './proveedor-formulario.html',
  styleUrl: './proveedor-formulario.css',
})
export class ProveedorFormulario implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly proveedorService = inject(ProveedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  private readonly nit = signal<number | null>(null);
  protected readonly modoEdicion = computed(() => this.nit() !== null);

  protected readonly formulario = this.fb.nonNullable.group({
    nitproveedor: ['', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]],
    nombreProveedor: ['', [Validators.required, Validators.maxLength(100)]],
    direccionProveedor: ['', [Validators.required, Validators.maxLength(150)]],
    telefonoProveedor: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]{7,20}$/)]],
    ciudadProveedor: ['', [Validators.required, Validators.maxLength(60)]],
  });

  ngOnInit(): void {
    const nitParam = this.route.snapshot.paramMap.get('nit');
    if (nitParam === null) {
      return;
    }

    const nit = Number(nitParam);
    this.nit.set(nit);
    this.formulario.controls.nitproveedor.disable();

    this.proveedorService.buscarPorNit(nit).subscribe((proveedor) => {
      this.formulario.patchValue({
        nitproveedor: String(proveedor.nitproveedor),
        nombreProveedor: proveedor.nombreProveedor,
        direccionProveedor: proveedor.direccionProveedor,
        telefonoProveedor: proveedor.telefonoProveedor,
        ciudadProveedor: proveedor.ciudadProveedor,
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
      ? this.proveedorService.actualizar(this.nit()!, {
          nombreProveedor: valores.nombreProveedor,
          direccionProveedor: valores.direccionProveedor,
          telefonoProveedor: valores.telefonoProveedor,
          ciudadProveedor: valores.ciudadProveedor,
        })
      : this.proveedorService.crear({
          nitproveedor: Number(valores.nitproveedor),
          nombreProveedor: valores.nombreProveedor,
          direccionProveedor: valores.direccionProveedor,
          telefonoProveedor: valores.telefonoProveedor,
          ciudadProveedor: valores.ciudadProveedor,
        });

    peticion.subscribe({
      next: () => this.router.navigateByUrl('/proveedores'),
      error: () => this.enviando.set(false),
    });
  }
}
