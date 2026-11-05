package com.hydrosmart.notifications.application.internal.outboundservices;
import java.util.Map;
public interface PushGateway {
 boolean configured();
 String publicKey();
 void validate(String endpoint,String p256dh,String auth);
 int send(Map<String,Object> subscription,Map<String,Object> payload);
}
