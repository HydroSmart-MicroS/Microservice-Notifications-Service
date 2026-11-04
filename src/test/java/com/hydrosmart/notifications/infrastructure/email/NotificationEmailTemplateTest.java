package com.hydrosmart.notifications.infrastructure.email;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;

class NotificationEmailTemplateTest {
 @Test void everyNotificationHasReadableDetailsAndTheCorrectAction()throws Exception{
  String[][] examples={
   {"high_consumption","Consumo de agua inusual","Se registraron 540 L en una ventana de 30 minutos. Tu umbral configurado es de 500 L.","/alertas"},
   {"possible_leak","Posible fuga de agua","Se detectó flujo continuo durante 3 minutos en el medidor Baño principal. Revisa el patrón de consumo de tu unidad.","/alertas"},
   {"goal_80","Tu meta mensual llegó al 80%","Llevas S/ 40.00 de tu presupuesto de S/ 50.00. Revisa tu consumo para mantenerte dentro de tu meta.","/alertas"},
   {"water_payment_pending","Pago de agua pendiente","Se generó un pago pendiente de agua de S/ 30.08 por octubre de 2026. Revisa su estado en Plan y pagos → Historial de pagos.","/pagos"},
   {"payment","Pago confirmado","Recibimos tu pago de S/ 40.00 por tu plan mensual.","/pagos"},
   {"report","Tu reporte está listo","Ya puedes descargar el reporte de consumo desde Historial.","/historial"},
   {"other","Tienes una nueva notificación","Hay una actualización en tu cuenta de HydroSmart.","/notificaciones"}
  };
  Path directory=Path.of("target/email-previews");Files.createDirectories(directory);
  for(var example:examples){
   var content=NotificationEmailTemplate.render(example[0],example[1],example[2],"http://localhost:8080/");
   assertTrue(content.html().contains("href=\"http://localhost:8080/app"+example[3]+"\""));
   assertTrue(content.text().contains(example[1]));assertTrue(content.text().contains(example[2]));assertTrue(content.text().contains("http://localhost:8080/app"+example[3]));
   assertTrue(content.html().contains("lang=\"es\""));assertTrue(content.html().contains("HYDROSMART"));assertFalse(content.html().contains("%%"));
   Files.writeString(directory.resolve(example[0]+".html"),content.html());
  }
 }
 @Test void eventTextCannotInjectHtmlOrLinks(){
  var content=NotificationEmailTemplate.render("<script>","<img src=x onerror=alert(1)>","Agua & consumo\n<script>alert('x')</script>","https://hydrosmart.example");
  assertFalse(content.html().contains("<script>"));assertFalse(content.html().contains("<img"));assertTrue(content.html().contains("&lt;script&gt;"));assertTrue(content.html().contains("Agua &amp; consumo<br>"));
  assertTrue(content.html().contains("href=\"https://hydrosmart.example/app/notificaciones\""));assertTrue(content.text().contains("Agua & consumo\n<script>"));
 }
 @Test void onlyConfiguredHttpAppLinksAreAllowed(){
  for(String url:new String[]{"javascript:alert(1)","/relative","https://user:secret@example.com","https://example.com?redirect=other","https://example.com#fragment",""})
   assertThrows(IllegalArgumentException.class,()->NotificationEmailTemplate.render("payment","Pago confirmado","Detalle",url));
 }
}
