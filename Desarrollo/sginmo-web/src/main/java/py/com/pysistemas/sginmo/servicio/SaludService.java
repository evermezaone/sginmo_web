package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * REQ-0051 - Salud operativa del sistema (solo lectura). Arma un tablero de indicadores
 * tecnicos con semaforo (OK/ADVERTENCIA/CRITICO) para soporte y operacion.
 *
 * Sin @AislarTenant a proposito: solo lee objetos sin RLS (flyway_schema_history, SELECT 1,
 * disco, JVM, manifiesto de backup); no toca tablas de negocio ni depende del tenant. Ningun
 * indicador expone credenciales ni rutas completas de secretos (solo nombres de archivo).
 */
@ApplicationScoped
@jakarta.transaction.Transactional
public class SaludService {

    public static final String OK = "OK";
    public static final String ADVERTENCIA = "ADVERTENCIA";
    public static final String CRITICO = "CRITICO";

    /** Umbrales de disco (porcentaje libre). Ajustables por entorno si se desea. */
    private static final int DISCO_CRIT = 5;
    private static final int DISCO_ADV = 15;
    /** Antiguedad maxima de un backup para considerarlo fresco (horas). */
    private static final long BACKUP_ADV_HORAS = 48;

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    /** Snapshot completo de indicadores + estado global (peor semaforo presente). */
    public Salud snapshot() {
        Salud s = new Salud();
        s.indicadores = new ArrayList<>();
        s.indicadores.add(baseDatos());
        s.indicadores.add(flyway());
        s.indicadores.add(disco());
        s.indicadores.add(memoria());
        s.indicadores.add(uptime());
        s.indicadores.add(backup());
        Build b = build();
        s.version = b.version;
        s.buildTime = b.time;
        s.commit = b.commit;
        s.estadoGlobal = peor(s.indicadores);
        return s;
    }

    // ── Indicadores ──────────────────────────────────────────────────────────

    /** Conexion a PostgreSQL + latencia aproximada de un SELECT 1. */
    private Indicador baseDatos() {
        try {
            long t0 = System.nanoTime();
            em.createNativeQuery("SELECT 1").getSingleResult();
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            String estado = ms < 200 ? OK : ADVERTENCIA;
            return new Indicador("Base de datos", "Conectada (" + ms + " ms)", estado,
                    "Latencia de un SELECT 1 contra PostgreSQL.");
        } catch (RuntimeException e) {
            return new Indicador("Base de datos", "Sin conexion", CRITICO,
                    "No respondio el SELECT 1. Revisar PostgreSQL y el datasource.");
        }
    }

    /** Estado de Flyway: version aplicada mas alta y migraciones fallidas. */
    private Indicador flyway() {
        try {
            Object ver = em.createNativeQuery(
                    "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")
                    .getSingleResult();
            Number fallidas = (Number) em.createNativeQuery(
                    "SELECT count(*) FROM flyway_schema_history WHERE success = false").getSingleResult();
            long f = fallidas == null ? 0 : fallidas.longValue();
            String v = ver == null ? "?" : ver.toString();
            if (f > 0) {
                return new Indicador("Migraciones (Flyway)", "V" + v + " · " + f + " fallida(s)", CRITICO,
                        "Hay migraciones marcadas success=false. Revisar flyway_schema_history.");
            }
            return new Indicador("Migraciones (Flyway)", "V" + v + " · sin fallidas", OK,
                    "Version de esquema aplicada mas alta.");
        } catch (RuntimeException e) {
            return new Indicador("Migraciones (Flyway)", "No disponible", ADVERTENCIA,
                    "No se pudo leer flyway_schema_history.");
        }
    }

    /** Espacio libre del disco donde corre el proceso (home del usuario del servidor). */
    private Indicador disco() {
        try {
            File raiz = new File(System.getProperty("user.home", "/"));
            long total = raiz.getTotalSpace();
            long libre = raiz.getUsableSpace();
            if (total <= 0) {
                return new Indicador("Disco", "No disponible", ADVERTENCIA, "No se pudo medir el disco.");
            }
            long pct = libre * 100L / total;
            String estado = pct < DISCO_CRIT ? CRITICO : (pct < DISCO_ADV ? ADVERTENCIA : OK);
            return new Indicador("Disco", pct + "% libre (" + gib(libre) + " / " + gib(total) + ")", estado,
                    "Espacio libre en la particion del servidor de aplicacion.");
        } catch (RuntimeException e) {
            return new Indicador("Disco", "No disponible", ADVERTENCIA, "No se pudo medir el disco.");
        }
    }

    /** Uso de heap de la JVM (WildFly). */
    private Indicador memoria() {
        Runtime r = Runtime.getRuntime();
        long max = r.maxMemory();
        long usada = r.totalMemory() - r.freeMemory();
        long pct = max > 0 ? usada * 100L / max : 0;
        String estado = pct > 90 ? ADVERTENCIA : OK;
        return new Indicador("Memoria JVM", pct + "% usada (" + mib(usada) + " / " + mib(max) + ")", estado,
                "Heap del proceso WildFly.");
    }

    /** Tiempo en linea del proceso. */
    private Indicador uptime() {
        long ms = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration d = Duration.ofMillis(ms);
        String txt = d.toDays() + "d " + (d.toHoursPart()) + "h " + (d.toMinutesPart()) + "m";
        return new Indicador("Uptime", txt, OK, "Tiempo desde el ultimo arranque de WildFly.");
    }

    /**
     * Ultimo backup valido segun el manifiesto de REQ-0065. Degrada a "sin datos" si no existe.
     * Ruta desde SGINMO_BACKUP_MANIFEST o, por defecto, ~/backups/latest.json.
     */
    private Indicador backup() {
        String ruta = System.getenv("SGINMO_BACKUP_MANIFEST");
        Path p = ruta != null && !ruta.isBlank()
                ? Path.of(ruta)
                : Path.of(System.getProperty("user.home", "."), "backups", "latest.json");
        if (!Files.isReadable(p)) {
            return new Indicador("Ultimo backup", "Sin datos", ADVERTENCIA,
                    "No se encontro el manifiesto de backup (REQ-0065 aun no programado).");
        }
        try {
            String json = Files.readString(p);
            String resultado = jsonStr(json, "resultado");
            String ts = jsonStr(json, "timestamp");
            OffsetDateTime cuando = ts != null ? OffsetDateTime.parse(ts) : null;
            boolean fresco = cuando != null
                    && Duration.between(cuando, OffsetDateTime.now()).toHours() <= BACKUP_ADV_HORAS;
            String estado;
            if (!"OK".equalsIgnoreCase(resultado)) {
                estado = CRITICO;
            } else {
                estado = fresco ? OK : ADVERTENCIA;
            }
            String detalleFecha = ts != null ? ts : "fecha desconocida";
            return new Indicador("Ultimo backup", (resultado == null ? "?" : resultado) + " · " + detalleFecha,
                    estado, fresco ? "Backup valido reciente." : "El ultimo backup supera las "
                            + BACKUP_ADV_HORAS + "h o fallo.");
        } catch (RuntimeException | java.io.IOException e) {
            return new Indicador("Ultimo backup", "Manifiesto ilegible", ADVERTENCIA,
                    "No se pudo interpretar el manifiesto de backup.");
        }
    }

    // ── Version / build ──────────────────────────────────────────────────────

    private Build build() {
        Build b = new Build();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("build-info.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                b.version = limpiar(props.getProperty("app.version"));
                b.time = limpiar(props.getProperty("app.build.time"));
                b.commit = limpiar(props.getProperty("app.build.commit"));
            }
        } catch (Exception ignore) {
            // degradacion elegante: sin sello de build
        }
        return b;
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    private static String peor(List<Indicador> ind) {
        boolean adv = false;
        for (Indicador i : ind) {
            if (CRITICO.equals(i.estado)) return CRITICO;
            if (ADVERTENCIA.equals(i.estado)) adv = true;
        }
        return adv ? ADVERTENCIA : OK;
    }

    /** Extrae el valor string de una clave de primer nivel de un JSON plano (sin lib externa). */
    private static String jsonStr(String json, String clave) {
        String needle = "\"" + clave + "\"";
        int k = json.indexOf(needle);
        if (k < 0) return null;
        int c = json.indexOf(':', k + needle.length());
        if (c < 0) return null;
        int i = c + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;   // solo valores string
        int start = i + 1;
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private static String limpiar(String v) {
        if (v == null) return null;
        v = v.trim();
        return (v.isEmpty() || "-".equals(v) || v.startsWith("${")) ? null : v;
    }

    private static String gib(long bytes) { return String.format("%.1f GiB", bytes / 1073741824.0); }
    private static String mib(long bytes) { return String.format("%.0f MiB", bytes / 1048576.0); }

    // ── DTOs ────────────────────────────────────────────────────────────────────

    public static class Indicador {
        public final String nombre;
        public final String valor;
        public final String estado;   // OK | ADVERTENCIA | CRITICO
        public final String detalle;
        public Indicador(String nombre, String valor, String estado, String detalle) {
            this.nombre = nombre; this.valor = valor; this.estado = estado; this.detalle = detalle;
        }
        public String getNombre() { return nombre; }
        public String getValor() { return valor; }
        public String getEstado() { return estado; }
        public String getDetalle() { return detalle; }
    }

    public static class Salud {
        public List<Indicador> indicadores;
        public String estadoGlobal;
        public String version;
        public String buildTime;
        public String commit;
        public List<Indicador> getIndicadores() { return indicadores; }
        public String getEstadoGlobal() { return estadoGlobal; }
        public String getVersion() { return version; }
        public String getBuildTime() { return buildTime; }
        public String getCommit() { return commit; }
    }

    private static class Build { String version; String time; String commit; }
}
