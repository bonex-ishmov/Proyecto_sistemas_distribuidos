package co.edu.tienda.catalogo.dto;

/** Resultado de una carga exitosa del archivo CSV. */
public record CargaResponse(
        String mensaje,
        int registrosEliminados,
        int registrosCargados
) {}
