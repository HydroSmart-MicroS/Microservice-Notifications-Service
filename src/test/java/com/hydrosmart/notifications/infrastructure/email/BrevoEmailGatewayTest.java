package com.hydrosmart.notifications.infrastructure.email;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;

class BrevoEmailGatewayTest {
 @Test void sendsHtmlAndPlainTextWithExistingSubjectAndIdempotency()throws Exception{
  var captured=new AtomicReference<Map>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{captured.set(RpcBus.JSON.readValue(exchange.getRequestBody().readAllBytes(),Map.class));var bytes="{\"messageId\":\"fake-mail-id\"}".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(201,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
  try{
   var gateway=new BrevoEmailGateway("isolated-key","sender@hydrosmart.invalid","http://127.0.0.1:"+server.getAddress().getPort(),"http://localhost:8080");
   assertEquals("fake-mail-id",gateway.send("recipient@hydrosmart.invalid","Pago confirmado","Recibimos S/ 40.00.","mail-reference","payment"));
   var payload=captured.get();assertEquals("Pago confirmado",payload.get("subject"));assertEquals("mail-reference",((Map)payload.get("headers")).get("IdempotencyKey"));
   assertTrue(payload.get("htmlContent").toString().contains("Ver mis pagos"));assertTrue(payload.get("textContent").toString().contains("http://localhost:8080/app/pagos"));
  }finally{server.stop(0);}
 }
}
