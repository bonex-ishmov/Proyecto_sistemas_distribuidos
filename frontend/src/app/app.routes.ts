import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.Login),
  },
  {
    // Un solo guard para todo lo protegido: cada pantalla nueva (clientes,
    // proveedores/productos, ventas, reportes) se agrega como hija acá, sin
    // repetir canActivate.
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./home/home').then((m) => m.Home) },
      {
        path: 'usuarios',
        loadComponent: () =>
          import('./usuarios/usuarios-lista/usuarios-lista').then((m) => m.UsuariosLista),
      },
      {
        path: 'usuarios/nuevo',
        loadComponent: () =>
          import('./usuarios/usuario-formulario/usuario-formulario').then(
            (m) => m.UsuarioFormulario,
          ),
      },
      {
        path: 'usuarios/:cedula/editar',
        loadComponent: () =>
          import('./usuarios/usuario-formulario/usuario-formulario').then(
            (m) => m.UsuarioFormulario,
          ),
      },
      {
        path: 'clientes',
        loadComponent: () =>
          import('./clientes/clientes-lista/clientes-lista').then((m) => m.ClientesLista),
      },
      {
        path: 'clientes/nuevo',
        loadComponent: () =>
          import('./clientes/cliente-formulario/cliente-formulario').then(
            (m) => m.ClienteFormulario,
          ),
      },
      {
        path: 'clientes/:cedula/editar',
        loadComponent: () =>
          import('./clientes/cliente-formulario/cliente-formulario').then(
            (m) => m.ClienteFormulario,
          ),
      },
      {
        path: 'proveedores',
        loadComponent: () =>
          import('./proveedores/proveedores-lista/proveedores-lista').then(
            (m) => m.ProveedoresLista,
          ),
      },
      {
        path: 'proveedores/nuevo',
        loadComponent: () =>
          import('./proveedores/proveedor-formulario/proveedor-formulario').then(
            (m) => m.ProveedorFormulario,
          ),
      },
      {
        path: 'proveedores/:nit/editar',
        loadComponent: () =>
          import('./proveedores/proveedor-formulario/proveedor-formulario').then(
            (m) => m.ProveedorFormulario,
          ),
      },
      {
        path: 'productos',
        loadComponent: () =>
          import('./productos/productos-lista/productos-lista').then((m) => m.ProductosLista),
      },
      {
        path: 'productos/cargar',
        loadComponent: () =>
          import('./productos/productos-carga/productos-carga').then((m) => m.ProductosCarga),
      },
      {
        path: 'ventas',
        loadComponent: () =>
          import('./ventas/venta-formulario/venta-formulario').then((m) => m.VentaFormulario),
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./reportes/reportes-inicio/reportes-inicio').then((m) => m.ReportesInicio),
      },
      {
        path: 'reportes/usuarios',
        loadComponent: () =>
          import('./reportes/reporte-usuarios/reporte-usuarios').then((m) => m.ReporteUsuarios),
      },
      {
        path: 'reportes/clientes',
        loadComponent: () =>
          import('./reportes/reporte-clientes/reporte-clientes').then((m) => m.ReporteClientes),
      },
      {
        path: 'reportes/ventas-por-cliente',
        loadComponent: () =>
          import('./reportes/reporte-ventas-por-cliente/reporte-ventas-por-cliente').then(
            (m) => m.ReporteVentasPorCliente,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
