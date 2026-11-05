package com.hydrosmart.notifications.interfaces.messaging;
import java.util.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
import com.hydrosmart.notifications.domain.services.NotificationService;
import com.hydrosmart.notifications.domain.services.BrowserPushService;
@Component
public class NotificationEventsController {
 private final NotificationService service;private final BrowserPushService push;
 public NotificationEventsController(NotificationService service){this(service,null);}
 @org.springframework.beans.factory.annotation.Autowired public NotificationEventsController(NotificationService service,BrowserPushService push){this.service=service;this.push=push;}
 @SuppressWarnings("unchecked")public void receive(Map<String,Object> event){
  var data=(Map<String,Object>)event.get("data");if(data==null||data.get("user_id")==null)throw new IllegalArgumentException("Invalid event");
  String kind,title,body,type=String.valueOf(event.get("event_type"));
  if(Set.of("analytics.leak.confirmed.v1","analytics.leak.dismissed.v1").contains(type)){
   String alert=UUID.fromString(data.get("alert_id").toString()).toString();
   push.recordDecision(data.get("user_id").toString(),alert,type.equals("analytics.leak.confirmed.v1")?"confirmed":"dismissed");return;
  }
  if(type.equals("billing.payment.confirmed.v1")){kind="payment";title="Pago confirmado";body="Recibimos tu pago de S/"+new BigDecimal(data.get("amount_minor").toString()).movePointLeft(2)+("plan".equals(data.get("kind"))?" por tu plan mensual.":" por el consumo de agua.");}
  else if(type.equals("billing.water.payment.pending.v1")){
   int amount=new BigDecimal(data.get("amount_minor").toString()).intValueExact();
   if(amount<=0||amount>100000000||!"PEN".equals(data.get("currency")))throw new IllegalArgumentException("Invalid water payment");
   UUID.fromString(data.get("order_id").toString());UUID.fromString(data.get("unit_id").toString());
   var month=YearMonth.parse(data.get("invoice_month").toString());
   kind="water_payment_pending";title="Pago de agua pendiente";
   body="Se generó un pago pendiente de agua de S/"+BigDecimal.valueOf(amount,2).toPlainString()+" por "+month.format(DateTimeFormatter.ofPattern("MMMM 'de' uuuu",Locale.forLanguageTag("es-PE")))+". Revisa su estado en Plan y pagos → Historial de pagos. Si ya completaste el pago, verifica su confirmación allí.";
  }
  else if(type.equals("reports.report.completed.v1")){kind="report";title="Tu reporte está listo";body="Ya puedes descargar el reporte de consumo desde Historial.";}
  else if(Set.of("analytics.alert.generated.v1","analytics.goal.threshold.v1").contains(type)){kind=String.valueOf(data.get("kind"));if(!Set.of("high_consumption","possible_leak","meter_offline","goal_80").contains(kind))throw new IllegalArgumentException("Invalid notice kind");title=String.valueOf(data.get("title"));body=String.valueOf(data.get("body"));}
  else throw new IllegalArgumentException("Unsupported notification event");
  var metadata=new HashMap<String,Object>();
  if(data.get("unit_id")!=null)metadata.put("unitId",UUID.fromString(data.get("unit_id").toString()).toString());
  if(data.get("alert_id")!=null)metadata.put("alertId",UUID.fromString(data.get("alert_id").toString()).toString());
  service.receive(new Notice(UUID.fromString(event.get("event_id").toString()),data.get("user_id").toString(),kind,title,body,metadata));
 }
 @SuppressWarnings("unchecked")public void email(Map<String,Object> event){
  var data=(Map<String,Object>)event.get("data");service.send(UUID.fromString(data.get("notification_id").toString()));
 }
 @SuppressWarnings("unchecked")public void push(Map<String,Object> event){var data=(Map<String,Object>)event.get("data");push.send(UUID.fromString(data.get("notification_id").toString()));}
}

