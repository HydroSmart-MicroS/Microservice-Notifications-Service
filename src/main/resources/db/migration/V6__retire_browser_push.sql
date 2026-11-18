UPDATE push_tokens SET revoked_at = now() WHERE revoked_at IS NULL;

UPDATE notifications SET status = 'skipped'
WHERE channel = 'push' AND status IN ('pending', 'failed');
