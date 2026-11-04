# Notifications

Bandeja, leído, preferencias, deduplicación de eventos y correo Brevo con reintentos.

DDD: application/internal, domain/model y domain/services, infrastructure, interfaces/acl e interfaces/rest.
La base es exclusiva de este contexto. Consultas de configuración y colaboración entre servicios usan RabbitMQ RPC;
los cambios pasan por outbox y los consumidores confirman después de procesar.

El Dockerfile compila y ejecuta sus pruebas. Iniciar desde la raíz de orquestación con .env privado.

[Funcionalidad y arranque](../README.md) · [Guía RabbitMQ](../docs/rabbitmq-guide.md) · [Contratos](../docs/contracts.md).

## Aviso de agua pendiente

Cada orden de consumo que queda pendiente produce un aviso con mes e importe en
la campanita/bandeja y, si está habilitado, por correo Brevo. Se usa
billing.water.payment.pending.v1 → notifications.alerts → notifications.email.
Se respeta inAppEnabled/emailEnabled; no depende de los avisos de reportes.
El aviso se genera una vez por orden, no al calcular consumo ni en cada reintento.
La actualización incluye pendientes existentes; pagados/anulados quedan excluidos.
El estado actual se consulta en Plan y pagos → Historial de pagos.

## Diseño del correo

Cada envío incluye HTML responsive y una alternativa de texto. La plantilla común
distingue alertas, metas, pagos y reportes, con enlaces a PUBLIC_APP_URL y textos
escapados. Conserva asunto, destinatario, preferencias, reintentos e idempotencia.
Las pruebas generan siete vistas previas; ver [diseño y validación](../docs/email-design.md).
