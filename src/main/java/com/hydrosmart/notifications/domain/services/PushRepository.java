package com.hydrosmart.notifications.domain.services;

import java.util.UUID;

public interface PushRepository {
    void retire(UUID notice);
    void recordDecision(String user, String alert, String decision);
}
