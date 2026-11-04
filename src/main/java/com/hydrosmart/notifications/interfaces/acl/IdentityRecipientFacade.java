package com.hydrosmart.notifications.interfaces.acl;
import org.springframework.stereotype.Component;
import com.hydrosmart.notifications.application.internal.outboundservices.RecipientDirectory;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
@Component
public class IdentityRecipientFacade implements RecipientDirectory {
 private final RpcBus bus;public IdentityRecipientFacade(RpcBus bus){this.bus=bus;}
 public String email(String user){try{return bus.get("identity","/internal/v1/users/"+java.net.URLEncoder.encode(user,java.nio.charset.StandardCharsets.UTF_8),bus.internalHeaders()).get("email").toString();}catch(RpcBus.BrokerStatus error){if(error.status==404)return null;throw error;}}
}

