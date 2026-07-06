package py.com.one.security.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Envio de correos con SMTP configurable via ProveedorParametros (claves SMTP_*).
 * Sin SMTP_HOST configurado, no envia y no molesta. El envio asincrono JAMAS
 * interrumpe el flujo que lo dispara (un fallo de correo se loguea y sigue).
 */
@ApplicationScoped
public class CorreoService {

    private static final Logger LOG = Logger.getLogger(CorreoService.class.getName());

    @Inject
    private Instance<ProveedorParametros> parametros;

    public void enviarAsync(String destinatario, String asunto, String cuerpo) {
        CompletableFuture.runAsync(() -> {
            try {
                enviar(destinatario, asunto, cuerpo);
            } catch (Exception e) {
                LOG.warning("No se pudo enviar el correo a " + destinatario + ": " + e.getMessage());
            }
        });
    }

    public void enviar(String destinatario, String asunto, String cuerpo) throws Exception {
        String host = parametro("SMTP_HOST");
        if (host == null || host.isBlank() || destinatario == null || destinatario.isBlank()) {
            return;   // SMTP no configurado o sin destinatario: envio deshabilitado
        }
        String usuario = parametro("SMTP_USUARIO");
        String clave = parametro("SMTP_CLAVE");
        String desde = valorODefecto(parametro("SMTP_DESDE"), usuario);
        String puerto = valorODefecto(parametro("SMTP_PUERTO"), "587");
        boolean tls = !"NO".equalsIgnoreCase(valorODefecto(parametro("SMTP_TLS"), "SI"));

        var props = new Properties();
        props.put("mail.smtp.host", host.trim());
        props.put("mail.smtp.port", puerto.trim());
        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session sesion;
        if (usuario != null && !usuario.isBlank()) {
            props.put("mail.smtp.auth", "true");
            final String u = usuario.trim();
            final String c = clave == null ? "" : clave;
            sesion = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(u, c);
                }
            });
        } else {
            sesion = Session.getInstance(props);
        }

        var mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(desde));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario.trim()));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setText(cuerpo, "UTF-8");
        Transport.send(mensaje);
    }

    private String parametro(String clave) {
        return parametros.isUnsatisfied() ? null : parametros.get().valor(clave);
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
