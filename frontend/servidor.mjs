// =====================================================================
// frontend/servidor.mjs  --  "FR NodeJS" del gráfico (front-1 / front-2)
//
// Servidor mínimo con SOLO los módulos que trae Node (sin dependencias):
//   1) /api/...  -> lo reenvía al balanceador del microservicio dueño de esa
//                   ruta, que a su vez elige una de sus 2 réplicas. En Docker es
//                   su NGINX (nginx-clientes...); en Kubernetes, su Service
//                   (ms-clientes...). Los nombres llegan por variables de entorno.
//   2) lo demás  -> archivos ya compilados por `ng build` (dist/.../browser).
//
// Reemplaza a `ng serve`: el servidor de desarrollo genera sus módulos con
// una huella distinta en cada copia, así que dos copias detrás de un
// balanceador se pisaban (504 "Outdated Optimize Dep"). Los archivos de
// `ng build` son idénticos en ambas réplicas, así que da igual cuál responda.
// =====================================================================
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const PUERTO = 4200;
const RAIZ = path.resolve('dist/frontend/browser');

// Balanceador de cada microservicio (siempre en el puerto 80). Sin variable de
// entorno se usa el NGINX del docker-compose.yml; los manifiestos de Kubernetes
// (k8s/) las ponen con el nombre del Service (DESTINO_CLIENTES=ms-clientes...).
const IDENTIDAD = process.env.DESTINO_IDENTIDAD ?? 'nginx-identidad';
const CLIENTES = process.env.DESTINO_CLIENTES ?? 'nginx-clientes';
const CATALOGO = process.env.DESTINO_CATALOGO ?? 'nginx-catalogo';
const VENTAS = process.env.DESTINO_VENTAS ?? 'nginx-ventas';
const REPORTES = process.env.DESTINO_REPORTES ?? 'nginx-reportes';

// Prefijo de la ruta -> balanceador. Misma tabla que antes vivía en proxy.docker.json.
const RUTAS_API = [
  ['/api/auth', IDENTIDAD],
  ['/api/usuarios', IDENTIDAD],
  ['/api/clientes', CLIENTES],
  ['/api/proveedores', CATALOGO],
  ['/api/productos', CATALOGO],
  ['/api/ventas', VENTAS],
  ['/api/reportes', REPORTES],
];

// Nombre de esta copia del front: en Kubernetes, el nombre del pod; en Docker, el
// id del contenedor. Va en la cabecera X-Atendido-Por-Front de cada respuesta,
// para poder ver qué front atendió (sin NGINX ya no está X-Atendido-Por).
const NOMBRE = os.hostname();

const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.ico': 'image/x-icon',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
  '.txt': 'text/plain; charset=utf-8',
};

function balanceadorPara(url) {
  const ruta = RUTAS_API.find(([prefijo]) => url === prefijo || url.startsWith(prefijo + '/') || url.startsWith(prefijo + '?'));
  return ruta ? ruta[1] : null;
}

// 1) Reenvío: misma petición (método, ruta, cabeceras y cuerpo) al balanceador,
// y su respuesta tal cual de vuelta al navegador. El cuerpo se pasa como flujo
// (pipe), así la carga CSV no se guarda entera en memoria.
//
// agent: false = una conexión nueva por petición. Por defecto Node reutiliza las
// conexiones (keep-alive), y un Service de Kubernetes reparte por CONEXIÓN, no por
// petición: con keep-alive casi todo iría siempre a la misma réplica. Así cada
// petición se reparte de nuevo, como hacía NGINX.
function reenviar(req, res, host) {
  const peticion = http.request(
    { host, port: 80, method: req.method, path: req.url, headers: req.headers, agent: false },
    (respuesta) => {
      res.writeHead(respuesta.statusCode, respuesta.headers);
      respuesta.pipe(res);
    },
  );
  peticion.on('error', () => {
    res.writeHead(502, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ mensaje: `No se pudo contactar a ${host}` }));
  });
  req.pipe(peticion);
}

// 2) Archivos estáticos.
function servirArchivo(req, res) {
  const ruta = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  let archivo = path.join(RAIZ, path.normalize(ruta));
  // Impide salir de RAIZ con rutas como /../../etc/passwd.
  if (!archivo.startsWith(RAIZ)) {
    res.writeHead(403).end();
    return;
  }
  // Rutas del router de Angular (/clientes, /ventas/nueva...) no son archivos:
  // se responde index.html y Angular decide qué pantalla mostrar.
  if (!fs.existsSync(archivo) || fs.statSync(archivo).isDirectory()) {
    if (path.extname(ruta)) {
      res.writeHead(404).end();
      return;
    }
    archivo = path.join(RAIZ, 'index.html');
  }
  res.writeHead(200, { 'Content-Type': TIPOS[path.extname(archivo)] ?? 'application/octet-stream' });
  fs.createReadStream(archivo).pipe(res);
}

http
  .createServer((req, res) => {
    res.setHeader('X-Atendido-Por-Front', NOMBRE);
    const host = balanceadorPara(req.url);
    if (host) reenviar(req, res, host);
    else servirArchivo(req, res);
  })
  .listen(PUERTO, () => console.log(`FR NodeJS ${NOMBRE} escuchando en el puerto ${PUERTO}, sirviendo ${RAIZ}; API -> ${[...new Set(RUTAS_API.map(([, h]) => h))].join(', ')}`));
