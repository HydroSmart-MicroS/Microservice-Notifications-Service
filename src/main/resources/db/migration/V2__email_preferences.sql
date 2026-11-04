ALTER TABLE notification_preferences ADD COLUMN email_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE notification_preferences ADD COLUMN in_app_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE notification_preferences ADD COLUMN goal_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE notifications ADD COLUMN kind TEXT NOT NULL DEFAULT 'alert';
ALTER TABLE notifications ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD COLUMN last_error TEXT;
ALTER TABLE notifications ADD COLUMN provider_message_id TEXT;

