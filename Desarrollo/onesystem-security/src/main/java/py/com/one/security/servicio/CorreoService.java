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
        if (destinatario == null || destinatario.isBlank()) {
            return;   // sin destinatario: nada que enviar
        }
        // REQ-0086: relay de correo por HTTP. Si hay MAIL_HTTP_URL configurado, el envio se delega a un
        // endpoint PHP en el hosting del dominio (envia como no-reply@dominio, pasa SPF/DKIM) via POST con
        // token. La app NUNCA guarda credenciales SMTP; solo el token. Sin MAIL_HTTP_URL, cae al SMTP directo.
        String relayUrl = parametro("MAIL_HTTP_URL");
        if (relayUrl != null && !relayUrl.isBlank()) {
            enviarPorHttp(relayUrl.trim(), valorODefecto(parametro("MAIL_HTTP_TOKEN"), ""),
                    destinatario.trim(), asunto, cuerpo);
            return;
        }
        String host = parametro("SMTP_HOST");
        if (host == null || host.isBlank()) {
            return;   // SMTP no configurado: envio deshabilitado
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

    /** REQ-0086: envia el correo delegando en el endpoint PHP (POST JSON {to,subject,body} + token). */
    private void enviarPorHttp(String url, String token, String to, String asunto, String cuerpo) throws Exception {
        String json = "{\"to\":\"" + jsonEsc(to) + "\",\"subject\":\"" + jsonEsc(asunto)
                + "\",\"body\":\"" + jsonEsc(cuerpo) + "\"}";
        var cliente = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10)).build();
        var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-Mailer-Token", token)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, java.nio.charset.StandardCharsets.UTF_8))
                .build();
        var resp = cliente.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new Exception("Relay de correo respondio HTTP " + resp.statusCode() + ": "
                    + (resp.body() == null ? "" : resp.body().substring(0, Math.min(200, resp.body().length()))));
        }
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        var sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }

    private String parametro(String clave) {
        return parametros.isUnsatisfied() ? null : parametros.get().valor(clave);
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
