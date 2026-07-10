package py.com.pysistemas.sginmo.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversion simple a letras para importes en guaranies (REQ-0041). */
public final class NumeroLetras {
    private NumeroLetras() { }

    private static final String[] UNIDADES = {
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete",
        "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres",
        "veinticuatro", "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve"
    };
    private static final String[] DECENAS = {
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };
    private static final String[] CENTENAS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    public static String guaranies(BigDecimal valor) {
        long n = valor == null ? 0 : valor.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (n == 0) return "cero guaranies";
        return apocoparUno(letras(n).trim()) + " guaranies";
    }

    private static String letras(long n) {
        if (n < 0) return "menos " + letras(-n);
        if (n < 30) return UNIDADES[(int) n];
        if (n < 100) {
            long d = n / 10, r = n % 10;
            return DECENAS[(int) d] + (r == 0 ? "" : " y " + letras(r));
        }
        if (n == 100) return "cien";
        if (n < 1000) {
            long c = n / 100, r = n % 100;
            return CENTENAS[(int) c] + (r == 0 ? "" : " " + letras(r));
        }
        if (n < 1_000_000) {
            long m = n / 1000, r = n % 1000;
            String pref = m == 1 ? "mil" : letras(m) + " mil";
            return pref + (r == 0 ? "" : " " + letras(r));
        }
        if (n < 1_000_000_000) {
            long m = n / 1_000_000, r = n % 1_000_000;
            String pref = m == 1 ? "un millon" : letras(m) + " millones";
            return pref + (r == 0 ? "" : " " + letras(r));
        }
        long mm = n / 1_000_000_000, r = n % 1_000_000_000;
        String pref = mm == 1 ? "mil millones" : letras(mm) + " mil millones";
        return pref + (r == 0 ? "" : " " + letras(r));
    }

    private static String apocoparUno(String texto) {
        if (texto.endsWith(" veintiuno")) return texto.substring(0, texto.length() - " veintiuno".length()) + " veintiun";
        if (texto.endsWith(" y uno")) return texto.substring(0, texto.length() - " y uno".length()) + " y un";
        if (texto.endsWith(" uno")) return texto.substring(0, texto.length() - " uno".length()) + " un";
        if ("uno".equals(texto)) return "un";
        return texto;
    }
}
