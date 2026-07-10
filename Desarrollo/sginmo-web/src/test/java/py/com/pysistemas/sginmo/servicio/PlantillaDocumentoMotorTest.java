package py.com.pysistemas.sginmo.servicio;

import org.junit.jupiter.api.Test;
import py.com.one.core.NegocioException;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlantillaDocumentoMotorTest {

    private final PlantillaDocumentoMotor motor = new PlantillaDocumentoMotor();

    @Test
    void reemplazaVariablesPermitidas() {
        String texto = motor.render("Contrato {{ cliente.nombre }} por {{operacion.numero}}",
                Map.of("cliente.nombre", "Juan Perez", "operacion.numero", "42"));

        assertEquals("Contrato Juan Perez por 42", texto);
    }

    @Test
    void rechazaVariablesNoCatalogadas() {
        assertThrows(NegocioException.class,
                () -> motor.validar("Hola {{cliente.password}}", Set.of("cliente.nombre")));
    }
}
