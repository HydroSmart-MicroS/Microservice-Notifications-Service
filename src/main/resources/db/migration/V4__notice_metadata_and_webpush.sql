ALTER TABLE notifications ADD COLUMN data JSONB NOT NULL DEFAULT '{}';
ALTER TABLE push_tokens ADD COLUMN p256dh TEXT;
ALTER TABLE push_tokens ADD COLUMN auth_secret TEXT;
CREATE TABLE push_deliveries(
 notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
 token_id UUID NOT NULL REFERENCES push_tokens(id) ON DELETE CASCADE,
 delivered_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(notification_id,token_id)
);
