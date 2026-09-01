import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal, type WritableSignal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { mensajeParaError } from '../../core/error.interceptor';
import { ProductoResponse } from '../../productos/producto.model';
import { ProductoService } from '../../productos/producto.service';
import { Avatar } from '../../shared/avatar/avatar';
import { LineaVentaRequest, VentaResponse } from '../venta.model';
import { VentaService } from '../venta.service';

const FILAS_INICIALES = 3;

/** Redondeo a 2 decimales (HALF_UP), igual que `escala()` en ms-ventas. */
function redondear2(valor: number): number {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

/**
 * Una fila del formulario: su FormGroup (código + cantidad) más el estado de
 * la previsualización (producto resuelto contra ms-catalogo, o el error de
 * esa línea puntual) y las suscripciones que hay que cortar si la fila se
 * quita del formulario.
 */
interface FilaVenta {
  grupo: FormGroup<{
    codigoProducto: FormControl<string>;
    cantidad: FormControl<string>;
  }>;
  producto: WritableSignal<ProductoResponse | null>;
  buscandoProducto: WritableSignal<boolean>;
  errorProducto: WritableSignal<string | null>;
  cantidadNumero: WritableSignal<number | null>;
  suscripciones: Subscription;
}

/**
 * Registro de venta (módulo 6): cédula del cliente + líneas de producto
 * (código + cantidad). Sin límite de líneas en el backend — el "máximo 3
 * productos" del PDF es solo la maqueta, por eso acá se arranca con 3 filas
 * pero se puede agregar más (decisión ya tomada, ver CLAUDE.md).
 *
 * Previsualización en vivo (nombre, precio, valor de línea y los 3 totales)
 * consultando GET /api/productos/{codigo} en ms-catalogo al escribir el
 * código. Es SOLO previsualización: el cálculo que vale y lo que se persiste
 * lo hace ms-ventas al confirmar — el frontend nunca manda precios ni
 * totales en el body de POST /api/ventas (ver RegistrarVentaRequest /
 * LineaVentaRequest: solo código y cantidad). Si el precio cambiara entre que
 * se escribe la línea y se confirma, o si el redondeo en JS difiere un
 * centavo del BigDecimal de ms-ventas, manda la respuesta del backend, que es
 * la que se muestra en la confirmación de abajo.
 */
@Component({
  selector: 'app-venta-formulario',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, Avatar],
  templateUrl: './venta-formulario.html',
  styleUrl: './venta-formulario.css',
})
export class VentaFormulario {
  private readonly fb = inject(FormBuilder);
  private readonly ventaService = inject(VentaService);
  private readonly productoService = inject(ProductoService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly enviando = signal(false);
  protected readonly errorLineas = signal<string | null>(null);
  protected readonly ventaRegistrada = signal<VentaResponse | null>(null);

  protected readonly cedulaCliente = this.fb.nonNullable.control('');
  protected lineas: FilaVenta[] = this.filasIniciales();

  private filasIniciales(): FilaVenta[] {
    return Array.from({ length: FILAS_INICIALES }, () => this.nuevaLinea());
  }

  private nuevaLinea(): FilaVenta {
    const grupo = this.fb.nonNullable.group({
      codigoProducto: this.fb.nonNullable.control(''),
      cantidad: this.fb.nonNullable.control(''),
    });

    const fila: FilaVenta = {
      grupo,
      producto: signal(null),
      buscandoProducto: signal(false),
      errorProducto: signal(null),
      cantidadNumero: signal(null),
      suscripciones: new Subscription(),
    };

    fila.suscripciones.add(
      grupo.controls.codigoProducto.valueChanges
        .pipe(debounceTime(300), distinctUntilChanged())
        .subscribe((valor) => this.buscarProducto(fila, valor)),
    );

    fila.suscripciones.add(
      grupo.controls.cantidad.valueChanges.subscribe((valor) => {
        const numero = Number(valor);
        fila.cantidadNumero.set(valor.trim() !== '' && Number.isFinite(numero) ? numero : null);
      }),
    );

    // Se cierran también si el componente entero se destruye (no solo al
    // quitar la fila con el botón "Quitar", ver quitarLinea).
    this.destroyRef.onDestroy(() => fila.suscripciones.unsubscribe());

    return fila;
  }

  private buscarProducto(fila: FilaVenta, codigoTexto: string): void {
    const codigo = codigoTexto.trim();
    fila.errorProducto.set(null);

    if (!/^[1-9]\d*$/.test(codigo)) {
      fila.producto.set(null);
      fila.buscandoProducto.set(false);
      return;
    }

    fila.buscandoProducto.set(true);
    this.productoService.buscarPorCodigo(Number(codigo)).subscribe({
      next: (producto) => {
        fila.producto.set(producto);
        fila.buscandoProducto.set(false);
      },
      error: (error: unknown) => {
        fila.producto.set(null);
        fila.buscandoProducto.set(false);
        fila.errorProducto.set(
          error instanceof HttpErrorResponse ? mensajeParaError(error) : 'No se pudo consultar el producto.',
        );
      },
    });
  }

  /** Valor de la línea SIN IVA (cantidad × precio de venta), o null si aún no hay producto/cantidad. */
  protected totalLinea(fila: FilaVenta): number | null {
    const producto = fila.producto();
    const cantidad = fila.cantidadNumero();
    if (!producto || cantidad === null || cantidad <= 0) {
      return null;
    }
    return redondear2(producto.precioVenta * cantidad);
  }

  /** IVA de la línea = valor de línea × (% IVA / 100), igual que ms-ventas. */
  protected ivaLinea(fila: FilaVenta): number | null {
    const total = this.totalLinea(fila);
    const producto = fila.producto();
    if (total === null || !producto) {
      return null;
    }
    return redondear2((total * producto.ivacompra) / 100);
  }

  /** Acumulado en vivo de los 3 totales del PDF, sumando solo las líneas con producto resuelto. */
  protected totalVentaAcumulado(): number {
    return redondear2(this.lineas.reduce((suma, fila) => suma + (this.totalLinea(fila) ?? 0), 0));
  }

  protected totalIvaAcumulado(): number {
    return redondear2(this.lineas.reduce((suma, fila) => suma + (this.ivaLinea(fila) ?? 0), 0));
  }

  protected totalConIvaAcumulado(): number {
    return redondear2(this.totalVentaAcumulado() + this.totalIvaAcumulado());
  }

  protected agregarLinea(): void {
    this.lineas = [...this.lineas, this.nuevaLinea()];
  }

  protected quitarLinea(indice: number): void {
    if (this.lineas.length <= 1) {
      return;
    }
    this.lineas[indice].suscripciones.unsubscribe();
    this.lineas = this.lineas.filter((_, i) => i !== indice);
  }

  protected registrar(): void {
    if (this.enviando()) {
      return;
    }

    const cedulaTexto = this.cedulaCliente.value.trim();
    if (!/^[1-9]\d*$/.test(cedulaTexto)) {
      this.errorLineas.set('La cédula del cliente es obligatoria y debe ser un número positivo.');
      return;
    }

    const detalles: LineaVentaRequest[] = this.lineas
      .map((fila) => fila.grupo.getRawValue())
      .filter((linea) => linea.codigoProducto.trim() !== '')
      .map((linea) => ({
        codigoProducto: Number(linea.codigoProducto),
        cantidad: Number(linea.cantidad),
      }));

    if (detalles.length === 0) {
      this.errorLineas.set('Agregá al menos un producto a la venta.');
      return;
    }
    const lineaInvalida = detalles.some(
      (d) =>
        !Number.isInteger(d.codigoProducto) ||
        d.codigoProducto <= 0 ||
        !Number.isInteger(d.cantidad) ||
        d.cantidad <= 0,
    );
    if (lineaInvalida) {
      this.errorLineas.set('Cada producto necesita un código y una cantidad válidos (mayores a cero).');
      return;
    }

    this.errorLineas.set(null);
    this.enviando.set(true);
    this.ventaRegistrada.set(null);

    this.ventaService
      .registrar({ cedulaCliente: Number(cedulaTexto), detalles })
      .subscribe({
        next: (venta) => {
          this.ventaRegistrada.set(venta);
          this.enviando.set(false);
          this.cedulaCliente.setValue('');
          this.lineas.forEach((fila) => fila.suscripciones.unsubscribe());
          this.lineas = this.filasIniciales();
        },
        // 404 (cliente inexistente), 409 (producto/proveedor inconsistente),
        // 503 (ms-clientes/ms-catalogo caídos) ya los muestra el banner global.
        error: () => this.enviando.set(false),
      });
  }
}
