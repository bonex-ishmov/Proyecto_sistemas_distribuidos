import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { ProductoResponse } from '../../productos/producto.model';
import { ProductoService } from '../../productos/producto.service';
import { VentaService } from '../venta.service';
import { VentaFormulario } from './venta-formulario';

const PRODUCTO: ProductoResponse = {
  codigoProducto: 5,
  nombreProducto: 'Arroz Diana 500g',
  nitproveedor: 1,
  nombreProveedor: 'Distribuidora X',
  precioCompra: 2100,
  ivacompra: 19,
  precioVenta: 2500,
};

describe('VentaFormulario', () => {
  let codigoConsultado: number | null;
  let respuestaProducto: Observable<ProductoResponse>;

  beforeEach(async () => {
    codigoConsultado = null;
    respuestaProducto = of(PRODUCTO);

    await TestBed.configureTestingModule({
      imports: [VentaFormulario],
      providers: [
        {
          provide: ProductoService,
          useValue: {
            buscarPorCodigo: (codigo: number) => {
              codigoConsultado = codigo;
              return respuestaProducto;
            },
          },
        },
        { provide: VentaService, useValue: { registrar: () => of(null) } },
      ],
    }).compileComponents();
  });

  function primerCodigo(fixture: ReturnType<typeof TestBed.createComponent<VentaFormulario>>) {
    // lineas es protected; en el test se accede por conveniencia.
    return (fixture.componentInstance as unknown as {
      lineas: { grupo: { controls: { codigoProducto: { setValue(v: string): void } } } }[];
    }).lineas[0].grupo.controls.codigoProducto;
  }

  // El valueChanges del código pasa por debounceTime(300); el proyecto es
  // zoneless (sin zone-testing) así que no hay fakeAsync: se espera con timers reales.
  const trasDebounce = () => new Promise((r) => setTimeout(r, 320));

  it('previsualiza nombre, valor unitario y subtotal al escribir un código válido', async () => {
    const fixture = TestBed.createComponent(VentaFormulario);
    fixture.detectChanges();

    primerCodigo(fixture).setValue('5');
    await trasDebounce();
    fixture.detectChanges();

    expect(codigoConsultado).toBe(5);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Arroz Diana 500g');
    // cantidad por defecto = 1 -> subtotal 2,500.00 y total con IVA (preview) 2,975.00
    expect(texto).toContain('2,500.00');
    expect(texto).toContain('2,975.00');
  });

  it('un código inexistente muestra el error en la fila, no en el banner global', async () => {
    respuestaProducto = throwError(
      () => new HttpErrorResponse({ status: 404, error: { mensaje: 'Producto Inexistente' } }),
    );

    const fixture = TestBed.createComponent(VentaFormulario);
    fixture.detectChanges();

    primerCodigo(fixture).setValue('999');
    await trasDebounce();
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Producto Inexistente');
  });
});
