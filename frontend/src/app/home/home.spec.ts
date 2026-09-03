import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ReporteService } from '../reportes/reporte.service';
import { ResumenDashboard } from '../reportes/resumen-dashboard.model';
import { Home } from './home';

const RESUMEN: ResumenDashboard = {
  productos: 42,
  clientes: 15,
  proveedores: 5,
  usuariosActivos: 3,
  ventas: {
    hoy: { cantidad: 3, montoTotalConIva: 154900 },
    mes: { cantidad: 27, montoTotalConIva: 1875300 },
  },
};

describe('Home', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        // Home solo necesita ReporteService (los routerLink no navegan en el test).
        { provide: ReporteService, useValue: { resumenDashboard: () => of(RESUMEN) } },
      ],
    }).compileComponents();
  });

  it('crea el componente', () => {
    const fixture = TestBed.createComponent(Home);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('pinta las métricas reales que devuelve ms-reportes', async () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Productos');
    expect(texto).toContain('42');
    expect(texto).toContain('Usuarios activos');
    // monto del mes formateado con DecimalPipe '1.2-2'
    expect(texto).toContain('1,875,300.00');
  });

  it('muestra el nombre solo si viene en el token (acá no hay sesión)', () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const titulo = (fixture.nativeElement as HTMLElement).querySelector('.hero-title');
    expect(titulo?.textContent?.trim()).toBe('¡Bienvenido!');
  });
});
