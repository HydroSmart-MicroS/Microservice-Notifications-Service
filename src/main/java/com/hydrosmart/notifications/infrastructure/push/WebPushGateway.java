package com.hydrosmart.notifications.infrastructure.push;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.security.Security;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import nl.martijndwars.webpush.*;
import org.apache.http.util.EntityUtils;
import com.hydrosmart.notifications.application.internal.outboundservices.PushGateway;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
@Component
public class WebPushGateway implements PushGateway {
 private final String publicKey,privateKey,subject,testUrl;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public WebPushGateway(@Value("${VAPID_PUBLIC_KEY:}")String publicKey,@Value("${VAPID_PRIVATE_KEY:}")String privateKey,
    @Value("${VAPID_SUBJECT:mailto:mateox341@gmail.com}")String subject,@Value("${WEB_PUSH_TEST_URL:}")String testUrl){
  this.publicKey=publicKey;this.privateKey=privateKey;this.subject=subject;this.testUrl=testUrl;
  if(Security.getProvider("BC")==null)Security.addProvider(new BouncyCastleProvider());
  if(!testUrl.isBlank()&&!Set.of("provider-stub","127.0.0.1","localhost").contains(URI.create(testUrl).getHost()))throw new IllegalArgumentException("Invalid isolated push target");
 }
 public boolean configured(){return !publicKey.isBlank()&&!privateKey.isBlank();}
 public String publicKey(){return configured()?publicKey:"";}
 public void validate(String endpoint,String p256dh,String auth){
  try{
   var uri=URI.create(endpoint);String host=uri.getHost();
   boolean provider=host!=null&&(host.equals("fcm.googleapis.com")||host.equals("web.push.apple.com")||host.endsWith(".push.services.mozilla.com")||host.endsWith(".notify.windows.com"));
   if(!provider||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||endpoint.length()>4096||uri.getRawPath().length()<2)throw new IllegalArgumentException();
   if(p256dh.length()>100||auth.length()>32)throw new IllegalArgumentException();
   var key=Base64.getUrlDecoder().decode(p256dh);if(key.length!=65||key[0]!=4||Base64.getUrlDecoder().decode(auth).length!=16)throw new IllegalArgumentException();
   Utils.loadPublicKey(p256dh);
  }catch(Exception error){throw new IllegalArgumentException("La suscripción del navegador no es válida. Vuelve a activar los avisos.");}
 }
 public int send(Map<String,Object> subscription,Map<String,Object> payload){
  try{
   String endpoint=subscription.get("token").toString(),p256dh=subscription.get("p256dh").toString(),auth=subscription.get("auth_secret").toString();validate(endpoint,p256dh,auth);
   var service=new nl.martijndwars.webpush.PushService(publicKey,privateKey,subject);
   var post=service.preparePost(new Notification(endpoint,p256dh,auth,RpcBus.JSON.writeValueAsString(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8),300),Encoding.AES128GCM);
   var request=java.net.http.HttpRequest.newBuilder(URI.create(testUrl.isBlank()?post.getURI().toString():testUrl)).timeout(Duration.ofSeconds(15));
   for(var header:post.getAllHeaders())request.header(header.getName(),header.getValue());
   return client.send(request.POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(EntityUtils.toByteArray(post.getEntity()))).build(),java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
  }catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException("Envío push interrumpido.");}
  catch(Exception error){throw new IllegalStateException("El servicio push no está disponible.");}
 }
}
