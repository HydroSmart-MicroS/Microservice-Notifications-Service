package com.hydrosmart.notifications.application.internal.commandservices;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.hydrosmart.notifications.domain.services.BrowserPushService;
import com.hydrosmart.notifications.domain.services.PushRepository;
import com.hydrosmart.notifications.application.internal.outboundservices.AccountGuard;

@Service
public class BrowserPushServiceImpl implements BrowserPushService {
    private final PushRepository repository;
    private final AccountGuard guard;

    public BrowserPushServiceImpl(PushRepository repository, AccountGuard guard) {
        this.repository = repository;
        this.guard = guard;
    }

    // Drain messages queued before browser push was removed.
    public void send(UUID id) {
        guard.delivery(id, () -> repository.retire(id));
    }

    public void recordDecision(String user, String alert, String decision) {
        repository.recordDecision(user, alert, decision);
    }
}
