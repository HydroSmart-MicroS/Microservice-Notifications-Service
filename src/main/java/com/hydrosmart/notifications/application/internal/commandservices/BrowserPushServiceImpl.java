package com.hydrosmart.notifications.application.internal.commandservices;
import java.util.*;
import org.springframework.stereotype.Service;
import com.hydrosmart.notifications.domain.services.*;
import com.hydrosmart.notifications.application.internal.outboundservices.*;
@Service
public class BrowserPushServiceImpl implements BrowserPushService {
 private final PushRepository repository;private final PushGateway gateway;private final AccountGuard guard;private final RecipientDirectory recipients;
 public BrowserPushServiceImpl(PushRepository repository,PushGateway gateway,AccountGuard guard,RecipientDirectory recipients){this.repository=repository;this.gateway=gateway;this.guard=guard;this.recipients=recipients;}
 public Map<String,Object> configuration(){return Map.of("configured",gateway.configured(),"publicKey",gateway.publicKey());}
 public void subscribe(String user,String endpoint,String p256dh,String auth){if(!gateway.configured())throw new IllegalArgumentException("Los avisos push todavía no están configurados.");gateway.validate(endpoint,p256dh,auth);repository.subscribe(user,endpoint,p256dh,auth);}
 public void unsubscribe(String user,String endpoint){if(endpoint==null||endpoint.length()>4096)throw new IllegalArgumentException("Suscripción inválida.");repository.unsubscribe(user,endpoint);}
 public void recordDecision(String user,String alert,String decision){repository.recordDecision(user,alert,decision);}
 public void send(UUID id){guard.read(()->{
  var notice=repository.notice(id);if(notice.isEmpty()||!"pending".equals(notice.get("status")))return;
  String user=notice.get("user_id").toString();if(recipients.email(user)==null)return;
  @SuppressWarnings("unchecked") var data=(Map<String,Object>)notice.getOrDefault("data",Map.of());
  var payload=new HashMap<String,Object>();payload.put("id",id.toString());payload.put("title",clip(notice.get("title").toString(),160));payload.put("body",clip(notice.get("body").toString(),600));payload.put("kind",notice.get("kind"));payload.put("data",data);
  boolean success=repository.hasDelivery(id),transientFailure=false;
  for(var subscription:repository.pending(id,user)){
   UUID subscriptionId=(UUID)subscription.get("id");
   try{int status=gateway.send(subscription,payload);
    if(status>=200&&status<300){repository.delivered(id,subscriptionId);success=true;}
    else if(status==404||status==410)repository.revoke(subscriptionId);
    else transientFailure=true;
   }catch(Exception error){transientFailure=true;}
  }
  if(transientFailure){repository.failed(id);repository.fallback(id);throw new IllegalStateException("Push retry required");}
  repository.finish(id,success);if(!success)repository.fallback(id);
 });}
 private static String clip(String value,int length){return value.length()>length?value.substring(0,length-1)+"…":value;}
}
