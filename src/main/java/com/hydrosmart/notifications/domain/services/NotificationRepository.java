package com.hydrosmart.notifications.domain.services;
import java.util.*;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
public interface NotificationRepository {
 void receive(Notice notice);
 default Map<String,Object> list(String user){return list(user,null);}
 Map<String,Object> list(String user,UUID before);
 Map<String,Object> preferences(String user);
 void preferences(String user,Map<String,Boolean> values);
 boolean read(String user,UUID id);
 Map<String,Object> email(UUID id);
 void sent(UUID id,String providerId);
 void failed(UUID id,String reason);
 void retry(String user);
}

