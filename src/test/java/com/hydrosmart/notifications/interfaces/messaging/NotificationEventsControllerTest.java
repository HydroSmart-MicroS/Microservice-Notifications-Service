package com.hydrosmart.notifications.interfaces.messaging;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
import com.hydrosmart.notifications.domain.services.NotificationService;
class NotificationEventsControllerTest {
 final NotificationService service=mock(NotificationService.class);
 final NotificationEventsController controller=new NotificationEventsController(service);
 Map<String,Object> data(){return new HashMap<>(Map.of("user_id","payer","order_id",UUID.randomUUID().toString(),"unit_id",UUID.randomUUID().toString(),"invoice_month","2026-09","amount_minor",3008,"currency","PEN"));}
 Map<String,Object> event(Map<String,Object> data){return Map.of("event_id",UUID.randomUUID().toString(),"event_type","billing.water.payment.pending.v1","data",data);}
 @Test void pendingWaterNoticeKeepsPayerSourceAndReadableAmountAndMonth(){
  var event=event(data());controller.receive(event);
  var notice=ArgumentCaptor.forClass(Notice.class);verify(service).receive(notice.capture());
  assertEquals(event.get("event_id"),notice.getValue().source().toString());
  assertEquals("payer",notice.getValue().user());assertEquals("water_payment_pending",notice.getValue().kind());
  assertEquals("Pago de agua pendiente",notice.getValue().title());
  assertTrue(notice.getValue().body().contains("S/30.08"));
  assertTrue(notice.getValue().body().contains("septiembre de 2026")||notice.getValue().body().contains("setiembre de 2026"));
  assertTrue(notice.getValue().body().contains("Historial de pagos"));
 }
 @Test void invalidAmountCurrencyOrMonthCannotCreateAnEmailNotice(){
  for(var entry:List.of(Map.entry("amount_minor",(Object)0),Map.entry("amount_minor",(Object)"1.5"),Map.entry("currency",(Object)"USD"),Map.entry("invoice_month",(Object)"2026-13"))){
   var data=data();data.put(entry.getKey(),entry.getValue());
   assertThrows(RuntimeException.class,()->controller.receive(event(data)));
  }
  verifyNoInteractions(service);
 }
}
