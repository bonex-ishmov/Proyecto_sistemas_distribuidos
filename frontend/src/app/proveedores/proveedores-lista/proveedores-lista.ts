import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ProveedorService } from '../proveedor.service';
import { ProveedorResponse } from '../proveedor.model';

/** Listado de proveedores (módulo 4). */
@Component({
  selector: 'app-proveedores-lista',
  imports: [RouterLink, Avatar],
  templateUrl: './proveedores-lista.html',
  styleUrl: './proveedores-lista.css',
})
export class ProveedoresLista implements OnInit {
  private readonly proveedorService = inject(ProveedorService);

  protected readonly proveedores = signal<ProveedorResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected eliminar(proveedor: ProveedorResponse): void {
    if (!confirm(`¿Eliminar al proveedor '${proveedor.nombreProveedor}'? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.proveedorService.eliminar(proveedor.nitproveedor).subscribe({
      next: () =>
        this.proveedores.update((lista) =>
          lista.filter((p) => p.nitproveedor !== proveedor.nitproveedor),
        ),
      // Si el proveedor tiene productos asociados, ms-catalogo responde 409 y
      // el banner global ya muestra el motivo.
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.proveedorService.listar().subscribe({
      next: (proveedores) => {
        this.proveedores.set(proveedores);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
