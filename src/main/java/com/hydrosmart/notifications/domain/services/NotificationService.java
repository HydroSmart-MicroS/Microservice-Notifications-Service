package com.hydrosmart.notifications.domain.services;
import java.util.Map;
import java.util.UUID;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
public interface NotificationService {
 void receive(Notice notice);
 boolean emailConfigured();
 void send(UUID id);
 default Map<String,Object> list(String user){return list(user,null);}
 Map<String,Object> list(String user,UUID before);
 Map<String,Object> preferences(String user);
 void preferences(String user,Map<String,Boolean> values);
 boolean read(String user,UUID id);
 void retry(String user);
}
