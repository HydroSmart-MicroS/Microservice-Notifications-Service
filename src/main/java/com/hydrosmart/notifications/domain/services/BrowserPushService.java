package com.hydrosmart.notifications.domain.services;
import java.util.*;
public interface BrowserPushService {
 Map<String,Object> configuration();
 void subscribe(String user,String endpoint,String p256dh,String auth);
 void unsubscribe(String user,String endpoint);
 void send(UUID id);
 void recordDecision(String user,String alert,String decision);
}
