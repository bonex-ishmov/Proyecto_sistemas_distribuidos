import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Avatar } from '../../shared/avatar/avatar';
import { ClienteService } from '../cliente.service';
import { ClienteResponse } from '../cliente.model';

/** Listado de clientes (módulo 3). */
@Component({
  selector: 'app-clientes-lista',
  imports: [RouterLink, Avatar],
  templateUrl: './clientes-lista.html',
  styleUrl: './clientes-lista.css',
})
export class ClientesLista implements OnInit {
  private readonly clienteService = inject(ClienteService);

  protected readonly clientes = signal<ClienteResponse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected eliminar(cliente: ClienteResponse): void {
    if (!confirm(`¿Eliminar al cliente '${cliente.nombreCliente}'? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.clienteService.eliminar(cliente.cedulaCliente).subscribe({
      next: () =>
        this.clientes.update((lista) =>
          lista.filter((c) => c.cedulaCliente !== cliente.cedulaCliente),
        ),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.clienteService.listar().subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
