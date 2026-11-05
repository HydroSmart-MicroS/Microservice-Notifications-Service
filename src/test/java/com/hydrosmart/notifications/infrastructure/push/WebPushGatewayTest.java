package com.hydrosmart.notifications.infrastructure.push;
import java.util.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.*;
class WebPushGatewayTest {
 String[] keys()throws Exception{
  new WebPushGateway("","","mailto:test@hydrosmart.invalid","");
  var generator=KeyPairGenerator.getInstance("ECDH","BC");generator.initialize(new ECGenParameterSpec("secp256r1"));var pair=generator.generateKeyPair();
  var encoder=Base64.getUrlEncoder().withoutPadding();
  return new String[]{encoder.encodeToString(Utils.encode((ECPublicKey)pair.getPublic())),encoder.encodeToString(((ECPrivateKey)pair.getPrivate()).getD().toByteArray())};
 }
 @Test void rejectsUntrustedEndpointsAndInvalidKeys()throws Exception{
  var key=keys();var gateway=new WebPushGateway(key[0],key[1],"mailto:test@hydrosmart.invalid","");
  String auth=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
  for(String endpoint:List.of("http://fcm.googleapis.com/a","https://127.0.0.1/private","https://fcm.googleapis.com.evil.example/a","https://fcm.googleapis.com:8443/a","https://user@fcm.googleapis.com/a"))assertThrows(IllegalArgumentException.class,()->gateway.validate(endpoint,key[0],auth));
  assertDoesNotThrow(()->gateway.validate("https://fcm.googleapis.com/fcm/send/test",key[0],auth));
  assertThrows(IllegalArgumentException.class,()->gateway.validate("https://fcm.googleapis.com/a","invalid",auth));
 }
 @Test void encryptsAndSignsPayloadWithoutContactingAnExternalProvider()throws Exception{
  var vapid=keys();var subscriber=keys();var bodies=new ArrayList<byte[]>();var headers=new ArrayList<com.sun.net.httpserver.Headers>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/push",exchange->{bodies.add(exchange.getRequestBody().readAllBytes());headers.add(exchange.getRequestHeaders());exchange.sendResponseHeaders(201,-1);exchange.close();});server.start();
  try{
   var gateway=new WebPushGateway(vapid[0],vapid[1],"mailto:test@hydrosmart.invalid","http://127.0.0.1:"+server.getAddress().getPort()+"/push");
   int code=gateway.send(Map.of("token","https://fcm.googleapis.com/fcm/send/test","p256dh",subscriber[0],"auth_secret",Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16])),Map.of("title","Posible fuga de agua"));
   assertEquals(201,code);assertEquals("aes128gcm",headers.getFirst().getFirst("Content-Encoding"));
   assertTrue(headers.getFirst().getFirst("Authorization").startsWith("vapid t="));
   assertTrue(bodies.getFirst().length>100);assertFalse(new String(bodies.getFirst(),java.nio.charset.StandardCharsets.ISO_8859_1).contains("Posible fuga"));
  }finally{server.stop(0);}
 }
}
