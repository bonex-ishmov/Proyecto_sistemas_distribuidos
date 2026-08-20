package co.edu.tienda.catalogo.service;

import co.edu.tienda.catalogo.dto.CargaResponse;
import co.edu.tienda.catalogo.entity.Producto;
import co.edu.tienda.catalogo.entity.Proveedor;
import co.edu.tienda.catalogo.exception.ArchivoInvalidoException;
import co.edu.tienda.catalogo.exception.DatosInvalidosException;
import co.edu.tienda.catalogo.exception.RecursoNoEncontradoException;
import co.edu.tienda.catalogo.repository.ProductoRepository;
import co.edu.tienda.catalogo.repository.ProveedorRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    /** codigo, nombre, nitproveedor, precio_compra, ivacompra, precio_venta */
    private static final int COLUMNAS_ESPERADAS = 6;
    private static final int MAX_ERRORES_REPORTADOS = 25;
    private static final int LONGITUD_MAX_NOMBRE = 50;

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoService(ProductoRepository productoRepository,
                           ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto buscarPorCodigo(Long codigo) {
        return productoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto Inexistente"));
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPorCodigos(List<Long> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return List.of();
        }
        return productoRepository.findByCodigoProductoIn(codigos);
    }

    // ==================== CARGA DEL ARCHIVO ====================

    /**
     * Flujo completo:
     *   1. Validar el archivo (existe, no vacio, extension .csv)
     *   2. Leerlo entero a memoria
     *   3. Descartar la fila de cabecera si la trae
     *   4. Validar TODAS las filas acumulando errores
     *   5. Si hay algun error -> abortar sin tocar la base de datos
     *   6. Si todo esta bien -> borrar lo anterior e insertar lo nuevo
     */
    @Transactional
    public CargaResponse cargar(MultipartFile archivo) {

        validarArchivo(archivo);

        List<String[]> filas = leerArchivo(archivo);

        if (filas.isEmpty()) {
            throw new ArchivoInvalidoException("Error: el archivo no contiene registros");
        }

        limpiarBom(filas.get(0));

        if (filas.get(0).length != COLUMNAS_ESPERADAS) {
            throw new ArchivoInvalidoException(
                    "Error: formato de archivo invalido. Se esperaban "
                            + COLUMNAS_ESPERADAS + " columnas separadas por comas y se encontraron "
                            + filas.get(0).length);
        }

        int primeraFilaDatos = esCabecera(filas.get(0)) ? 1 : 0;

        if (filas.size() - primeraFilaDatos == 0) {
            throw new ArchivoInvalidoException(
                    "Error: el archivo solo contiene la cabecera, sin registros");
        }

        List<Producto> productos = validarFilas(filas, primeraFilaDatos);

        return reemplazarCatalogo(productos);
    }

    // ---------- Paso 1 ----------

    private void validarArchivo(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Error: no se selecciono archivo para cargar");
        }

        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
            throw new ArchivoInvalidoException(
                    "Error: formato de archivo invalido. El archivo debe tener extension .csv");
        }
    }

    // ---------- Paso 2 ----------

    private List<String[]> leerArchivo(MultipartFile archivo) {
        try (Reader lector = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(lector)) {

            return csvReader.readAll().stream()
                    .filter(this::filaNoVacia)
                    .toList();

        } catch (IOException | CsvException ex) {
            throw new ArchivoInvalidoException(
                    "Error: formato de archivo invalido. No fue posible leer el contenido");
        }
    }

    private boolean filaNoVacia(String[] fila) {
        return fila.length > 0
                && Arrays.stream(fila).anyMatch(celda -> celda != null && !celda.isBlank());
    }

    // ---------- Paso 3 ----------

    /** Algunos editores agregan un caracter invisible (BOM) al inicio del archivo. */
    private void limpiarBom(String[] primeraFila) {
        if (primeraFila.length > 0 && primeraFila[0] != null) {
            primeraFila[0] = primeraFila[0].replace("\uFEFF", "").trim();
        }
    }

    /** Si la primera celda no es un numero, la fila es la cabecera. */
    private boolean esCabecera(String[] fila) {
        try {
            Long.parseLong(fila[0].trim());
            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    // ---------- Paso 4 ----------

    private List<Producto> validarFilas(List<String[]> filas, int desde) {

        List<String> errores = new ArrayList<>();
        Map<Long, String[]> filasValidas = new LinkedHashMap<>();
        Set<Long> nitsReferenciados = new HashSet<>();

        for (int i = desde; i < filas.size(); i++) {
            int numeroLinea = i + 1;
            String[] fila = filas.get(i);

            if (fila.length != COLUMNAS_ESPERADAS) {
                errores.add("Linea " + numeroLinea + ": se esperaban "
                        + COLUMNAS_ESPERADAS + " columnas y se encontraron " + fila.length);
                continue;
            }

            Long codigo = aLong(fila[0]);
            if (codigo == null || codigo <= 0) {
                errores.add("Linea " + numeroLinea
                        + ": el codigo de producto debe ser un numero entero positivo");
                continue;
            }
            if (filasValidas.containsKey(codigo)) {
                errores.add("Linea " + numeroLinea
                        + ": el codigo de producto " + codigo + " esta duplicado en el archivo");
                continue;
            }

            String nombre = fila[1] == null ? "" : fila[1].trim();
            if (nombre.isEmpty()) {
                errores.add("Linea " + numeroLinea + ": el nombre del producto es obligatorio");
                continue;
            }
            if (nombre.length() > LONGITUD_MAX_NOMBRE) {
                errores.add("Linea " + numeroLinea + ": el nombre del producto supera "
                        + LONGITUD_MAX_NOMBRE + " caracteres");
                continue;
            }

            Long nit = aLong(fila[2]);
            if (nit == null || nit <= 0) {
                errores.add("Linea " + numeroLinea
                        + ": el NIT del proveedor debe ser un numero entero positivo");
                continue;
            }

            BigDecimal precioCompra = aDecimal(fila[3]);
            if (precioCompra == null || precioCompra.signum() < 0) {
                errores.add("Linea " + numeroLinea
                        + ": el precio de compra debe ser un numero mayor o igual a cero");
                continue;
            }

            BigDecimal iva = aDecimal(fila[4]);
            if (iva == null || iva.signum() < 0 || iva.compareTo(BigDecimal.valueOf(100)) > 0) {
                errores.add("Linea " + numeroLinea
                        + ": el IVA debe ser un porcentaje entre 0 y 100");
                continue;
            }

            BigDecimal precioVenta = aDecimal(fila[5]);
            if (precioVenta == null || precioVenta.signum() < 0) {
                errores.add("Linea " + numeroLinea
                        + ": el precio de venta debe ser un numero mayor o igual a cero");
                continue;
            }

            filasValidas.put(codigo, fila);
            nitsReferenciados.add(nit);
        }

        // RF-08: validar los proveedores con UNA sola consulta, no una por fila.
        Map<Long, Proveedor> proveedores = proveedorRepository
                .findByNitproveedorIn(new ArrayList<>(nitsReferenciados))
                .stream()
                .collect(Collectors.toMap(Proveedor::getNitproveedor, Function.identity()));

        List<Producto> productos = new ArrayList<>();

        for (Map.Entry<Long, String[]> entrada : filasValidas.entrySet()) {
            String[] fila = entrada.getValue();
            Long nit = aLong(fila[2]);
            Proveedor proveedor = proveedores.get(nit);

            if (proveedor == null) {
                errores.add("Producto " + entrada.getKey()
                        + ": el proveedor con NIT " + nit + " no existe en la base de datos");
                continue;
            }

            productos.add(new Producto(
                    entrada.getKey(),
                    fila[1].trim(),
                    proveedor,
                    aDecimal(fila[3]),
                    aDecimal(fila[4]),
                    aDecimal(fila[5])
            ));
        }

        if (!errores.isEmpty()) {
            List<String> reportados = errores.size() > MAX_ERRORES_REPORTADOS
                    ? new ArrayList<>(errores.subList(0, MAX_ERRORES_REPORTADOS))
                    : errores;
            if (errores.size() > MAX_ERRORES_REPORTADOS) {
                reportados.add("... y " + (errores.size() - MAX_ERRORES_REPORTADOS)
                        + " error(es) mas");
            }
            throw new DatosInvalidosException("Error: datos leidos invalidos", reportados);
        }

        return productos;
    }

    private Long aLong(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    private BigDecimal aDecimal(String texto) {
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    // ---------- Paso 5 ----------

    /**
     * Decision A-3: la carga REEMPLAZA el catalogo anterior.
     * Al estar dentro de @Transactional, si el insert falla,
     * el delete tambien se revierte. Nunca queda la tabla vacia por error.
     */
    private CargaResponse reemplazarCatalogo(List<Producto> productos) {

        int eliminados = (int) productoRepository.count();
        productoRepository.deleteAllInBatch();
        productoRepository.flush();

        List<Producto> guardados = productoRepository.saveAll(productos);

        log.info("Carga de productos: {} eliminados, {} insertados.",
                eliminados, guardados.size());

        return new CargaResponse(
                "Archivo Cargado Exitosamente", eliminados, guardados.size());
    }
}
