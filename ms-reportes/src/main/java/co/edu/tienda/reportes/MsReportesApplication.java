package co.edu.tienda.reportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Modulo 7 del PDF: consultas y reportes.
 *
 * ms-reportes NO persiste nada: es un agregador de solo lectura. Cada consulta
 * se resuelve llamando por HTTP a ms-identidad, ms-clientes y/o ms-ventas y
 * combinando sus respuestas. Como no hay DataSource en el classpath, Spring Boot
 * no autoconfigura JPA y no hace falta excluir nada.
 */
@SpringBootApplication
public class MsReportesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsReportesApplication.class, args);
    }
}
