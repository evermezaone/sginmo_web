<?php
/**
 * REQ-0086 - Relay de correo transaccional para SGInmo (sin dependencias).
 * Recibe un POST JSON { "to", "subject", "body" } con el header X-Mailer-Token y envia el correo
 * como no-reply@one.com.py usando la funcion mail() nativa de PHP (el MTA local del hosting, que
 * envia con el dominio y pasa SPF/DKIM). La VPS/app solo conoce el token, nunca las credenciales.
 *
 * Subir a: https://one.com.py/mailer/send.php   (carpeta public_html/mailer/)
 * NO requiere Composer ni PHPMailer: es un unico archivo.
 */

// ===================== CONFIGURACION =====================
const MAILER_TOKEN   = 'REEMPLAZAR_POR_EL_TOKEN';  // el mismo valor que MAIL_HTTP_TOKEN en la app. NO subir al repo.
const MAIL_FROM      = 'no-reply@one.com.py';
const MAIL_FROM_NAME = 'SGInmo';
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
$body    = str_replace("\r\n", "\n", (string)($d['body'] ?? ''));

if (!filter_var($to, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'invalid_recipient']);
    exit;
}

// Asunto en UTF-8 codificado (RFC 2047) para no romper acentos/enes.
$subjectEnc = '=?UTF-8?B?' . base64_encode($subject) . '?=';

// From sanitizado (el nombre no debe contener CR/LF).
$fromName = str_replace(["\r", "\n"], ' ', MAIL_FROM_NAME);
$headers  = 'From: ' . $fromName . ' <' . MAIL_FROM . '>' . "\r\n";
$headers .= 'Reply-To: ' . MAIL_FROM . "\r\n";
$headers .= 'MIME-Version: 1.0' . "\r\n";
$headers .= 'Content-Type: text/plain; charset=UTF-8' . "\r\n";
$headers .= 'Content-Transfer-Encoding: 8bit' . "\r\n";
$headers .= 'X-Mailer: SGInmo-Relay';

// El 5to parametro fuerza el envelope-from al dominio (mejora la entrega/SPF).
$ok = @mail($to, $subjectEnc, $body, $headers, '-f' . MAIL_FROM);

if ($ok) {
    echo json_encode(['ok' => true]);
} else {
    $err = error_get_last();
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'mail_failed', 'detail' => $err['message'] ?? null]);
}
