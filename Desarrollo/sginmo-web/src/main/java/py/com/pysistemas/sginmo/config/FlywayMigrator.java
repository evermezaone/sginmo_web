package py.com.pysistemas.sginmo.config;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Migraciones Flyway al arranque (REQ-0032): las migraciones de db/migration dejan de
 * aplicarse a mano. Tecnica de adopcion sobre BD existente:
 *  - baselineOnMigrate + baselineVersion=21: una BD NO vacia sin historia (la VPS actual,
 *    con V1..V21 ya aplicadas a mano) se marca baseline en 21 y solo corre V22+.
 *  - una BD VACIA (entorno nuevo) corre TODAS las migraciones desde V1.
 * validateOnMigrate=false para tolerar las que ya estaban aplicadas manualmente.
 */
@Singleton
@Startup
public class FlywayMigrator {

    private static final Logger LOG = Logger.getLogger(FlywayMigrator.class.getName());

    @Resource(lookup = "java:/jdbc/SGInmoDS")
    private DataSource dataSource;

    /** NOT_SUPPORTED: corre sin transaccion JTA para que Flyway controle el autocommit. */
    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void migrar() {
        try {
            var flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("21")
                    .validateOnMigrate(false)
                    .load();
            var res = flyway.migrate();
            LOG.info("Flyway: " + res.migrationsExecuted + " migracion(es) aplicada(s); esquema en "
                    + (res.targetSchemaVersion == null ? "baseline" : res.targetSchemaVersion));
        } catch (Exception e) {
            // no tumbar el arranque por Flyway: loguear y seguir (la app funciona con el esquema actual)
            LOG.warning("Flyway no pudo migrar (se continua con el esquema existente): " + e.getMessage());
        }
    }
}
