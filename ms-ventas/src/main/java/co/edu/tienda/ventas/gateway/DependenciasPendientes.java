package co.edu.tienda.ventas.gateway;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementacion PROVISIONAL de los tres puertos, valida solo para el
 * Incremento 2. Permite que el contexto de Spring arranque (hay un bean que
 * satisface CatalogoGateway, ClientesGateway e IdentidadGateway) sin abrir
 * todavia ninguna llamada HTTP.
 *
 * El registro de ventas end-to-end NO funciona hasta el Incremento 4, donde
 * esta clase se reemplaza por adaptadores HTTP reales. Los tests de
 * VentaService no la usan: mockean las interfaces.
 */
@Component
public class DependenciasPendientes
        implements CatalogoGateway, ClientesGateway {

    private static final String MENSAJE =
            "Integracion HTTP con las dependencias pendiente hasta el Incremento 4";

    @Override
    public List<ProductoInfo> buscarProductos(List<Long> codigos) {
        throw new UnsupportedOperationException(MENSAJE);
    }

    @Override
    public boolean existeCliente(Long cedulaCliente) {
        throw new UnsupportedOperationException(MENSAJE);
    }
}
