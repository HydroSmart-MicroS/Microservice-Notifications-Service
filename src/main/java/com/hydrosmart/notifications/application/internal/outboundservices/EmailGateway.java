package com.hydrosmart.notifications.application.internal.outboundservices;
public interface EmailGateway {
 boolean configured();
 String send(String recipient,String title,String body,String reference,String kind);
 default String send(String recipient,String title,String body,String reference,String kind,java.util.Map<String,Object> data){return send(recipient,title,body,reference,kind);}
}

