package com.hydrosmart.notifications.application.internal.outboundservices;
public interface EmailGateway {
 boolean configured();
 String send(String recipient,String title,String body,String reference,String kind);
}

