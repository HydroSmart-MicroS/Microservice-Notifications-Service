package com.hydrosmart.notifications.domain.services;
import java.util.*;
public interface PushRepository {
 void subscribe(String user,String endpoint,String p256dh,String auth);
 void unsubscribe(String user,String endpoint);
 Map<String,Object> notice(UUID id);
 List<Map<String,Object>> pending(UUID notice,String user);
 void delivered(UUID notice,UUID subscription);
 boolean hasDelivery(UUID notice);
 void failed(UUID notice);
 void revoke(UUID subscription);
 void finish(UUID notice,boolean success);
 void fallback(UUID notice);
 void recordDecision(String user,String alert,String decision);
}
