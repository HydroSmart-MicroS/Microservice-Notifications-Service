package com.hydrosmart.notifications.domain.services;

import java.util.UUID;

public interface BrowserPushService {
    void send(UUID id);
    void recordDecision(String user, String alert, String decision);
}
