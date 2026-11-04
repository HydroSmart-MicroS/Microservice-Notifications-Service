package com.hydrosmart.notifications.interfaces.rest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import com.hydrosmart.notifications.domain.services.NotificationService;
import com.hydrosmart.notifications.interfaces.rest.resources.NotificationPreferencesResource;
@RestController
public class NotificationsController {
 private final NotificationService service;
 public NotificationsController(NotificationService service){this.service=service;}
 @GetMapping("/api/v1/notifications")public Map<String,Object> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)UUID before){return service.list(jwt.getSubject(),before);}
 @PostMapping("/api/v1/notifications/{id}/read")public Map<String,Object> read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){if(!service.read(jwt.getSubject(),id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return Map.of("read",true);}
 @GetMapping("/api/v1/notification-preferences")public NotificationPreferencesResource prefs(@AuthenticationPrincipal Jwt jwt){return NotificationPreferencesResource.from(service.preferences(jwt.getSubject()));}
 @PutMapping("/api/v1/notification-preferences")public NotificationPreferencesResource prefs(@AuthenticationPrincipal Jwt jwt,@RequestBody Map<String,Boolean> body){
  var resource=NotificationPreferencesResource.from(body);
  service.preferences(jwt.getSubject(),resource.values());return NotificationPreferencesResource.from(service.preferences(jwt.getSubject()));
 }
 @ExceptionHandler(IllegalArgumentException.class)public ResponseEntity<?> invalid(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("detail",error.getMessage()));}
 @PostMapping("/api/v1/notifications/email/retry")public Map<String,Object> retry(@AuthenticationPrincipal Jwt jwt){service.retry(jwt.getSubject());return Map.of("queued",true);}
}

