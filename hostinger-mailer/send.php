<?php
/**
 * REQ-0086 - Relay de correo transaccional para SGInmo.
 * Recibe un POST JSON { "to", "subject", "body" } con el header X-Mailer-Token y envia el correo
 * como no-reply@one.com.py usando PHPMailer. Corre en el hosting del dominio (Hostinger), asi el
 * envio pasa SPF/DKIM. La VPS/app solo conoce el token (nunca las credenciales SMTP).
 *
 * Subir a: https://one.com.py/mailer/send.php
 * Requiere PHPMailer: o bien `composer require phpmailer/phpmailer` en esta carpeta (crea vendor/),
 * o bien subir los archivos en ./PHPMailer/src/ (PHPMailer.php, SMTP.php, Exception.php).
 */

// ===================== CONFIGURACION =====================
const MAILER_TOKEN   = 'REEMPLAZAR_POR_EL_TOKEN';  // pega aca el token (el mismo que MAIL_HTTP_TOKEN en la app). NO lo subas al repo.
const MAIL_FROM      = 'no-reply@one.com.py';
const MAIL_FROM_NAME = 'SGInmo';

// SMTP opcional. Si SMTP_HOST queda vacio, se usa la funcion mail() de PHP (el MTA local del hosting,
// que envia como el dominio). Si preferis SMTP autenticado con la casilla no-reply, completa estos:
const SMTP_HOST = '';                 // ej: 'smtp.hostinger.com'
const SMTP_USER = 'no-reply@one.com.py';
const SMTP_PASS = '';                 // contrasena de la casilla no-reply (solo si usas SMTP)
const SMTP_PORT = 587;
// =========================================================

header('Content-Type: application/json; charset=utf-8');

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'method_not_allowed']);
    exit;
}

// Autenticacion por token (comparacion a prueba de timing).
$token = $_SERVER['HTTP_X_MAILER_TOKEN'] ?? '';
if (!is_string($token) || !hash_equals(MAILER_TOKEN, $token)) {
    http_response_code(401);
    echo json_encode(['ok' => false, 'error' => 'unauthorized']);
    exit;
}

$raw = file_get_contents('php://input');
$d = json_decode($raw, true);
if (!is_array($d)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'bad_json']);
    exit;
}

$to      = trim((string)($d['to'] ?? ''));
$subject = str_replace(["\r", "\n"], ' ', (string)($d['subject'] ?? ''));  // anti header-injection
$body    = (string)($d['body'] ?? '');

if (!filter_var($to, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'invalid_recipient']);
    exit;
}

// Carga de PHPMailer (composer autoload o carpeta manual).
if (is_file(__DIR__ . '/vendor/autoload.php')) {
    require __DIR__ . '/vendor/autoload.php';
} elseif (is_file(__DIR__ . '/PHPMailer/src/PHPMailer.php')) {
    require __DIR__ . '/PHPMailer/src/PHPMailer.php';
    require __DIR__ . '/PHPMailer/src/SMTP.php';
    require __DIR__ . '/PHPMailer/src/Exception.php';
} else {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'phpmailer_missing']);
    exit;
}

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

try {
    $mail = new PHPMailer(true);
    if (SMTP_HOST !== '') {
        $mail->isSMTP();
        $mail->Host       = SMTP_HOST;
        $mail->Port       = SMTP_PORT;
        $mail->SMTPAuth   = true;
        $mail->Username   = SMTP_USER;
        $mail->Password   = SMTP_PASS;
        $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    } else {
        $mail->isMail();
    }
    $mail->CharSet = 'UTF-8';
    $mail->setFrom(MAIL_FROM, MAIL_FROM_NAME);
    $mail->addAddress($to);
    $mail->Subject = $subject;
    $mail->Body    = $body;   // texto plano (SGInmo envia texto)
    $mail->send();
    echo json_encode(['ok' => true]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => isset($mail) ? $mail->ErrorInfo : $e->getMessage()]);
}
