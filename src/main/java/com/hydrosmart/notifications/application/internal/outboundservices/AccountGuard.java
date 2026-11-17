package com.hydrosmart.notifications.application.internal.outboundservices;

import java.util.UUID;

public interface AccountGuard {
 void read(Runnable work);
 default void delivery(UUID id, Runnable work) { read(work); }
}
