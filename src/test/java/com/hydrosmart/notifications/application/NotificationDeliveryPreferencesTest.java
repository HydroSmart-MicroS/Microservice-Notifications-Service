package com.hydrosmart.notifications.application;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import com.hydrosmart.notifications.application.internal.commandservices.NotificationCommandServiceImpl;
import com.hydrosmart.notifications.application.internal.outboundservices.EmailGateway;
import com.hydrosmart.notifications.application.internal.outboundservices.RecipientDirectory;
import com.hydrosmart.notifications.domain.services.NotificationRepository;

class NotificationDeliveryPreferencesTest {
    final NotificationRepository repository=mock(NotificationRepository.class);
    final EmailGateway emails=mock(EmailGateway.class);
    final RecipientDirectory recipients=mock(RecipientDirectory.class);
    final NotificationCommandServiceImpl service=new NotificationCommandServiceImpl(repository,recipients,emails,Runnable::run);
    final UUID id=UUID.randomUUID();

    void queued(String kind) {
        when(repository.email(id)).thenReturn(Map.of("status","pending","user_id","owner","kind",kind,"title","Aviso","body","Revisa tu agua","data",Map.of()));
        when(recipients.email("owner")).thenReturn("qa@example.invalid");
    }
    @Test void disablingEmailSkipsAnAlreadyQueuedAlert() {
        queued("high_consumption");when(repository.preferences("owner")).thenReturn(Map.of("emailEnabled",false,"highConsumptionEnabled",true));
        service.send(id);verify(repository).skipped(id);verifyNoInteractions(emails);
    }
    @Test void disablingTheAlertTypeSkipsItsQueuedEmail() {
        queued("possible_leak");when(repository.preferences("owner")).thenReturn(Map.of("emailEnabled",true,"leakAlertsEnabled",false));
        service.send(id);verify(repository).skipped(id);verifyNoInteractions(emails);
    }
    @Test void disablingReportEmailsSkipsAQueuedSummary() {
        queued("report");when(repository.preferences("owner")).thenReturn(Map.of("emailEnabled",true,"summariesEnabled",false));
        service.send(id);verify(repository).skipped(id);verifyNoInteractions(emails);
    }
    @Test void enabledEmailStillDelivers() {
        queued("high_consumption");when(repository.preferences("owner")).thenReturn(Map.of("emailEnabled",true,"highConsumptionEnabled",true));
        service.send(id);verify(emails).send(eq("qa@example.invalid"),anyString(),anyString(),eq(id.toString()),eq("high_consumption"),anyMap());
    }
    @Test void skippedMailIsTerminal() {
        when(repository.email(id)).thenReturn(Map.of("status","skipped"));
        service.send(id);verifyNoInteractions(emails,recipients);
    }


    @Test void requestedPasswordResetBypassesEmailPreferences() {
        when(repository.email(id)).thenReturn(Map.of("status","pending","user_id","owner","kind","password_reset","title","Restablece","body","Enlace","data",Map.of("expiresAt",java.time.Instant.now().plusSeconds(300).toString())));
        when(recipients.email("owner")).thenReturn("qa@example.invalid");
        service.send(id);verify(repository,never()).preferences(anyString());verify(emails).send(anyString(),anyString(),anyString(),anyString(),eq("password_reset"),anyMap());
    }
}
