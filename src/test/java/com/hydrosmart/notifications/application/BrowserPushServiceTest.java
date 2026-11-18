package com.hydrosmart.notifications.application;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import com.hydrosmart.notifications.domain.services.PushRepository;
import com.hydrosmart.notifications.application.internal.commandservices.BrowserPushServiceImpl;

class BrowserPushServiceTest {
    @Test
    void queuedPushIsRetiredAndLeakDecisionsStillUpdateNotificationHistory() {
        var repository = mock(PushRepository.class);
        var service = new BrowserPushServiceImpl(repository, Runnable::run);
        UUID id = UUID.randomUUID();
        service.send(id);
        service.recordDecision("owner", "alert", "confirmed");
        verify(repository).retire(id);
        verify(repository).recordDecision("owner", "alert", "confirmed");
        verifyNoMoreInteractions(repository);
    }
}
