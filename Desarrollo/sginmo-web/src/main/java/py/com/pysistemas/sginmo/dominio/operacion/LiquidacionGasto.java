package py.com.pysistemas.sginmo.dominio.operacion;

import java.io.Serializable;
import java.math.BigDecimal;

/** Renglon de gasto de una liquidacion (fila de UI sobre liquidacion_detalle). */
public class LiquidacionGasto implements Serializable {
    private Long id;            // liquidacion_detalle id (null si nuevo)
    private Long articulo;
    private String concepto;
    private BigDecimal monto = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticulo() { return articulo; }
    public void setArticulo(Long v) { this.articulo = v; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String v) { this.concepto = v; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal v) { this.monto = v; }
}
