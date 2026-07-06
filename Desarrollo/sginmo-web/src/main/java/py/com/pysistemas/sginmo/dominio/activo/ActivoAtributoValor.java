package py.com.pysistemas.sginmo.dominio.activo;

import java.io.Serializable;

/**
 * Fila de la pestana Atributos del ABM de activos (no es entidad): combina la definicion
 * (atributo por tipo, obligatoriedad) con el valor cargado para ese activo.
 */
public class ActivoAtributoValor implements Serializable {

    private Long activoAtributoId;   // null si aun no tiene valor guardado
    private Long atributoId;
    private String descripcion;
    private String tipoDato;         // TEXTO | NUMERO | BOOLEANO | FECHA
    private boolean obligatorio;
    private String valor;

    public Long getActivoAtributoId() { return activoAtributoId; }
    public void setActivoAtributoId(Long v) { this.activoAtributoId = v; }
    public Long getAtributoId() { return atributoId; }
    public void setAtributoId(Long v) { this.atributoId = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public String getTipoDato() { return tipoDato; }
    public void setTipoDato(String v) { this.tipoDato = v; }
    public boolean isObligatorio() { return obligatorio; }
    public void setObligatorio(boolean v) { this.obligatorio = v; }
    public String getValor() { return valor; }
    public void setValor(String v) { this.valor = v; }
}
