package co.edu.tienda.ventas.gateway;

import java.util.List;

/**
 * Puerto hacia ms-catalogo. En el Incremento 2 solo existe el contrato:
 * el service depende de esta interfaz y los tests la mockean.
 * La implementacion real (HTTP contra POST /api/productos/lote, autenticada
 * con el JWT del usuario de sistema) llega en el Incremento 4.
 */
public interface CatalogoGateway {

    /**
     * Devuelve los productos existentes para los codigos pedidos.
     * Puede devolver MENOS elementos que los solicitados si algun codigo
     * no existe en el catalogo; el que valida eso es VentaService.
     */
    List<ProductoInfo> buscarProductos(List<Long> codigos);
}
