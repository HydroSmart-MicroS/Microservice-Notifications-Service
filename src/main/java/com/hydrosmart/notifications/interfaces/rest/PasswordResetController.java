package com.hydrosmart.notifications.interfaces.rest;
import java.util.*;
import java.net.URI;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.hydrosmart.notifications.domain.services.NotificationService;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
@RestController
public class PasswordResetController {
 private final NotificationService service;private final URI origin;
 public PasswordResetController(NotificationService service,@Value("${PUBLIC_APP_URL:http://localhost:8080}")String origin){this.service=service;this.origin=URI.create(origin);}
 @PostMapping("/internal/v1/password-reset") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
 public Map<String,Object> queue(@RequestBody Map<String,String> body){
  var link=URI.create(body.getOrDefault("actionUrl",""));
  var expiry=Instant.parse(body.getOrDefault("expiresAt",""));
  if(!Objects.equals(link.getScheme(),origin.getScheme())||!Objects.equals(link.getAuthority(),origin.getAuthority())||
     !link.getPath().startsWith("/api/auth/reset-password/")||link.getUserInfo()!=null||link.toString().length()>4096||
     expiry.isAfter(Instant.now().plusSeconds(1900)))throw new IllegalArgumentException("Invalid account email");
  if(expiry.isAfter(Instant.now()))service.receive(new Notice(UUID.fromString(body.get("source")),body.get("userId"),"password_reset",
   "Restablece tu contraseña de HydroSmart","Recibimos una solicitud para cambiar tu contraseña. El enlace dura 30 minutos y solo se puede usar una vez. Si no lo solicitaste, ignora este correo.",
   Map.of("actionUrl",link.toString(),"expiresAt",expiry.toString())));
  return Map.of("queued",true);
 }
}
