package com.hydrosmart.notifications.infrastructure.messaging.rabbitmq;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
import com.hydrosmart.notifications.interfaces.messaging.NotificationEventsController;
import com.hydrosmart.notifications.domain.services.NotificationService;
@Component
public class NotificationConsumers {
 private final RpcBus bus;private final NotificationEventsController controller;private final NotificationService service;
 public NotificationConsumers(RpcBus bus,NotificationEventsController controller,NotificationService service){this.bus=bus;this.controller=controller;this.service=service;}
 @EventListener(ApplicationReadyEvent.class)public void start()throws Exception{
  bus.consume("notifications.alerts",List.of("analytics.alert.generated.v1","analytics.goal.threshold.v1","billing.payment.confirmed.v1","billing.water.payment.pending.v1","reports.report.completed.v1"),controller::receive);
  if(service.emailConfigured())bus.consume("notifications.email",List.of("notifications.email.requested.v1"),controller::email);
 }
}

