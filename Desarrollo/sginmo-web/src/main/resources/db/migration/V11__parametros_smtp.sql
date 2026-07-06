-- V11 (ONEsystem-security): parametros SMTP configurables para alertas de acceso
-- fallido (decision del usuario: SMTP de one.com.py, datos configurables).
-- SMTP_USUARIO / SMTP_CLAVE / SMTP_DESDE se completan con las credenciales reales;
-- con SMTP_HOST vacio el envio queda deshabilitado sin molestar.
INSERT INTO parametro_sistema (clave, valor, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('SMTP_HOST',             'mail.one.com.py', 'Servidor SMTP para correos del sistema (vacio = deshabilitado)', 'sistema', now()),
  ('SMTP_PUERTO',           '587',             'Puerto SMTP',                                                    'sistema', now()),
  ('SMTP_USUARIO',          '',                'Usuario SMTP (completar)',                                       'sistema', now()),
  ('SMTP_CLAVE',            '',                'Clave SMTP (completar)',                                         'sistema', now()),
  ('SMTP_DESDE',            '',                'Remitente de los correos (vacio = igual al usuario SMTP)',       'sistema', now()),
  ('SMTP_TLS',              'SI',              'Usar STARTTLS (SI/NO)',                                          'sistema', now()),
  ('ALERTA_LOGIN_FALLIDO',  'SI',              'Enviar alerta por correo ante intento de acceso fallido (SI/NO)','sistema', now());
