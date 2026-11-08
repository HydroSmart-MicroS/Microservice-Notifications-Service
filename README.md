# Notifications

Bandeja, leído, preferencias, deduplicación de eventos y correo Brevo con reintentos.

DDD: application/internal, domain/model y domain/services, infrastructure, interfaces/acl e interfaces/rest.
La base es exclusiva de este contexto. Consultas de configuración y colaboración entre servicios usan RabbitMQ RPC;
los cambios pasan por outbox y los consumidores confirman después de procesar.

## Operación en MAIN

En la red de Compose, el servicio escucha en `notifications:8080` y utiliza la base `hs_notifications`.

| Comprobación | Ruta interna |
|---|---|
| Proceso | `GET /actuator/health/liveness` |
| Disponibilidad | `GET /actuator/health/readiness` |

Readiness verifica PostgreSQL y RabbitMQ. Revisa el resultado en el [panel de MAIN](http://127.0.0.1:8099/). Para diagnosticar un correo o push, revisa el estado de la entrega y sus reintentos. La comprobación de salud valida las dependencias del servicio. La aceptación del proveedor se verifica en el registro de entrega.

Construye y ejecuta las pruebas del Dockerfile desde este clon con `docker build -t hydrosmart-notifications .`. Para la prueba conjunta, ejecuta `./scripts/Test-Local.ps1 -DatabaseMode Hosted` desde la carpeta de orquestación.

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
