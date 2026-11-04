CREATE TABLE notification_preferences (
    user_id TEXT PRIMARY KEY,
    high_consumption_enabled BOOLEAN NOT NULL DEFAULT true,
    leak_alerts_enabled BOOLEAN NOT NULL DEFAULT true,
    summaries_enabled BOOLEAN NOT NULL DEFAULT false,
    timezone TEXT NOT NULL DEFAULT 'America/Lima',
    quiet_start TIME,
    quiet_end TIME,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((quiet_start IS NULL) = (quiet_end IS NULL))
);
CREATE TABLE push_tokens (
    id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ
);
CREATE INDEX push_tokens_user_idx ON push_tokens (user_id);
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    source_event_id UUID NOT NULL,
    channel TEXT NOT NULL CHECK (channel IN ('in_app','push','email')),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','sent','failed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    UNIQUE (user_id, source_event_id, channel)
);
CREATE INDEX notifications_user_idx ON notifications (user_id, created_at DESC);


CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    event_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0)
);
CREATE INDEX outbox_pending_idx ON outbox_events (occurred_at) WHERE published_at IS NULL;
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
