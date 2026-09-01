import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ProductoService } from '../producto.service';
import { ProductoResponse } from '../producto.model';

/**
 * Listado de productos (módulo 5), de solo lectura: crear/editar/eliminar
 * productos individuales no existe en el backend, solo la carga CSV
 * (ver `productos-carga`).
 */
@Component({
  selector: 'app-productos-lista',
  imports: [RouterLink, DecimalPipe, Avatar],
  templateUrl: './productos-lista.html',
  styleUrl: './productos-lista.css',
})
export class ProductosLista implements OnInit {
  private readonly productoService = inject(ProductoService);

  protected readonly productos = signal<ProductoResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
