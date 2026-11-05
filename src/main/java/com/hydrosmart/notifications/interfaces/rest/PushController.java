package com.hydrosmart.notifications.interfaces.rest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.hydrosmart.notifications.domain.services.BrowserPushService;
@RestController
public class PushController {
 private final BrowserPushService service;
 public PushController(BrowserPushService service){this.service=service;}
 @GetMapping("/api/v1/notifications/push/config") public Map<String,Object> configuration(){return service.configuration();}
 @PostMapping("/api/v1/notifications/push/subscriptions") public Map<String,Object> subscribe(@AuthenticationPrincipal Jwt jwt,@RequestBody Map<String,Object> body){
  if(!(body.get("endpoint") instanceof String endpoint)||!(body.get("keys") instanceof Map<?,?> keys)||!(keys.get("p256dh") instanceof String p256dh)||!(keys.get("auth") instanceof String auth))throw new IllegalArgumentException("Suscripción de navegador inválida.");
  service.subscribe(jwt.getSubject(),endpoint,p256dh,auth);return Map.of("subscribed",true);
 }
 @DeleteMapping("/api/v1/notifications/push/subscriptions") public Map<String,Object> unsubscribe(@AuthenticationPrincipal Jwt jwt,@RequestBody Map<String,String> body){service.unsubscribe(jwt.getSubject(),body.getOrDefault("endpoint",""));return Map.of("subscribed",false);}
 @ExceptionHandler(IllegalArgumentException.class)public org.springframework.http.ResponseEntity<?> invalid(IllegalArgumentException error){return org.springframework.http.ResponseEntity.badRequest().body(Map.of("detail",error.getMessage()));}
}
