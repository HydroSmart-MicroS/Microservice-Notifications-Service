package com.hydrosmart.notifications.infrastructure.email;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.hydrosmart.notifications.application.internal.outboundservices.EmailGateway;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
@Component
public class BrevoEmailGateway implements EmailGateway {
 private final String key,sender,url,publicUrl;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public BrevoEmailGateway(@Value("${BREVO_API_KEY:}")String key,@Value("${BREVO_SENDER_EMAIL:mateox341@gmail.com}")String sender,@Value("${BREVO_API_URL:https://api.brevo.com/v3/smtp/email}")String url,@Value("${PUBLIC_APP_URL:http://localhost:8080}")String publicUrl){this.key=key;this.sender=sender;this.url=url;this.publicUrl=publicUrl;}
 public boolean configured(){return !key.isBlank()&&!sender.isBlank();}
 public String send(String email,String title,String body,String reference,String kind){
  return send(email,title,body,reference,kind,Map.of());
 }
 public String send(String email,String title,String body,String reference,String kind,Map<String,Object> data){
  if(!configured())throw new IllegalStateException("Brevo no está configurado.");
  try{
   var content=NotificationEmailTemplate.render(kind,title,body,publicUrl,data);
   var payload=Map.of("sender",Map.of("name","HydroSmart","email",sender),"to",List.of(Map.of("email",email)),"subject",title,
     "textContent",content.text(),"htmlContent",content.html(),
     "headers",Map.of("X-Mailin-custom","hydrosmart:"+reference,"IdempotencyKey",reference));
   var request=HttpRequest.newBuilder(URI.create(url)).header("api-key",key).header("Content-Type","application/json").timeout(Duration.ofSeconds(20))
    .POST(HttpRequest.BodyPublishers.ofString(RpcBus.JSON.writeValueAsString(payload))).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=201)throw new IllegalStateException("Brevo respondió "+response.statusCode()+".");
   var result=RpcBus.JSON.readValue(response.body(),Map.class);
   if(result.get("messageId")==null)throw new IllegalStateException("Brevo no confirmó la aceptación.");
   return result.get("messageId").toString();
  }catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException("Envío interrumpido.");}
  catch(IllegalStateException error){throw error;}
  catch(Exception error){throw new IllegalStateException("Brevo no está disponible.");}
 }
}

