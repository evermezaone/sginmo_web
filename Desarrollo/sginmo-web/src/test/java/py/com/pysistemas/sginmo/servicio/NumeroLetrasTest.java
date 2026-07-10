package py.com.pysistemas.sginmo.servicio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumeroLetrasTest {

    @Test
    void convierteGuaraniesBasicos() {
        assertEquals("cero guaranies", NumeroLetras.guaranies(BigDecimal.ZERO));
        assertEquals("un guaranies", NumeroLetras.guaranies(BigDecimal.ONE));
        assertEquals("un millon doscientos treinta y cuatro mil quinientos sesenta y siete guaranies",
                NumeroLetras.guaranies(new BigDecimal("1234567")));
    }
}
