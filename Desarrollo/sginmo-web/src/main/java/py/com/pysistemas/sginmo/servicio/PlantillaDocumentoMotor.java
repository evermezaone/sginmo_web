package py.com.pysistemas.sginmo.servicio;

import py.com.one.core.NegocioException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Parser seguro de plantillas: solo reemplaza placeholders del catalogo cerrado. */
public class PlantillaDocumentoMotor {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    public record Variable(String codigo, String descripcion) { }

    public Set<String> extraer(String cuerpo) {
        var vars = new LinkedHashSet<String>();
        if (cuerpo == null) return vars;
        var m = VAR.matcher(cuerpo);
        while (m.find()) {
            vars.add(m.group(1));
        }
        return vars;
    }

    public void validar(String cuerpo, Set<String> permitidas) {
        var desconocidas = new ArrayList<String>();
        for (String var : extraer(cuerpo)) {
            if (!permitidas.contains(var)) {
                desconocidas.add(var);
            }
        }
        if (!desconocidas.isEmpty()) {
            throw new NegocioException("Variables desconocidas en la plantilla: " + String.join(", ", desconocidas));
        }
    }

    public String render(String cuerpo, Map<String, String> valores) {
        if (cuerpo == null) return "";
        var m = VAR.matcher(cuerpo);
        var sb = new StringBuffer();
        while (m.find()) {
            String valor = valores.getOrDefault(m.group(1), "");
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(valor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public List<Variable> variablesDisponibles() {
        return List.of(
            new Variable("empresa.razon_social", "Razon social de la empresa emisora"),
            new Variable("empresa.ruc", "RUC/documento de la empresa emisora"),
            new Variable("sucursal.descripcion", "Sucursal de la operacion"),
            new Variable("operacion.numero", "Numero interno de operacion"),
            new Variable("operacion.tipo", "Tipo de operacion: alquiler o venta"),
            new Variable("operacion.fecha_inicio", "Fecha de inicio del contrato"),
            new Variable("operacion.fecha_fin", "Fecha de fin del contrato"),
            new Variable("operacion.plazo", "Plazo en meses/cuotas"),
            new Variable("operacion.monto_total", "Monto total en numeros"),
            new Variable("operacion.monto_total_letras", "Monto total en letras"),
            new Variable("operacion.precio", "Precio mensual o total segun la operacion"),
            new Variable("operacion.garantia", "Garantia de alquiler"),
            new Variable("cliente.nombre", "Nombre del cliente/deudor"),
            new Variable("cliente.documento", "Documento del cliente/deudor"),
            new Variable("deudor.nombre", "Nombre del deudor del pagare"),
            new Variable("deudor.documento", "Documento del deudor del pagare"),
            new Variable("acreedor.nombre", "Nombre del acreedor del pagare"),
            new Variable("activo.descripcion", "Descripcion del activo/propiedad"),
            new Variable("activo.direccion", "Direccion del activo/propiedad"),
            new Variable("tipo_contrato.descripcion", "Descripcion del tipo de contrato"),
            new Variable("cuota.numero", "Numero de cuota del pagare"),
            new Variable("cuota.vencimiento", "Fecha de vencimiento de la cuota"),
            new Variable("cuota.monto", "Monto de la cuota en numeros"),
            new Variable("cuota.monto_letras", "Monto de la cuota en letras")
        );
    }

    public Set<String> codigosPermitidos() {
        return variablesDisponibles().stream()
            .map(Variable::codigo)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
