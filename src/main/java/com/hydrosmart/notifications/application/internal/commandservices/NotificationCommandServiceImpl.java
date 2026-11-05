package com.hydrosmart.notifications.application.internal.commandservices;
import java.util.*;
import org.springframework.stereotype.Service;
import com.hydrosmart.notifications.domain.services.NotificationService;
import com.hydrosmart.notifications.domain.services.NotificationRepository;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
import com.hydrosmart.notifications.application.internal.outboundservices.*;
@Service
public class NotificationCommandServiceImpl implements NotificationService {
 private final NotificationRepository repository;private final RecipientDirectory recipients;private final EmailGateway emails;private final AccountGuard guard;
 public NotificationCommandServiceImpl(NotificationRepository repository,RecipientDirectory recipients,EmailGateway emails,AccountGuard guard){this.repository=repository;this.recipients=recipients;this.emails=emails;this.guard=guard;}
 public void receive(Notice notice){guard.read(()->{if(recipients.email(notice.user())!=null)repository.receive(notice);});}
 public boolean emailConfigured(){return emails.configured();}
 public void send(UUID id){
  guard.read(()->sendLocked(id));
 }
 private void sendLocked(UUID id){
  var row=repository.email(id);if(row.isEmpty()||"sent".equals(row.get("status")))return;
  try{String email=recipients.email(row.get("user_id").toString());if(email==null)return;
   @SuppressWarnings("unchecked") var data=(Map<String,Object>)row.getOrDefault("data",Map.of());
   if("password_reset".equals(row.get("kind"))&&java.time.Instant.parse(data.get("expiresAt").toString()).isBefore(java.time.Instant.now())){repository.expired(id);return;}
   String provider=emails.send(email,row.get("title").toString(),row.get("body").toString(),id.toString(),row.get("kind").toString(),data);repository.sent(id,provider);}
  catch(Exception error){repository.failed(id,error.getMessage());throw new IllegalStateException("Email retry required");}
 }
 public Map<String,Object> list(String user,UUID before){var value=new HashMap<>(repository.list(user,before));value.put("emailConfigured",emails.configured());return value;}
 public Map<String,Object> preferences(String user){return repository.preferences(user);}
 public void preferences(String user,Map<String,Boolean> values){repository.preferences(user,values);}
 public boolean read(String user,UUID id){return repository.read(user,id);}
 public void retry(String user){repository.retry(user);}
}

