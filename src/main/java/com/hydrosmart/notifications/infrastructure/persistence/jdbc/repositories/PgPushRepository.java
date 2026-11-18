package com.hydrosmart.notifications.infrastructure.persistence.jdbc.repositories;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.hydrosmart.notifications.domain.services.PushRepository;

@Repository
public class PgPushRepository implements PushRepository {
    private final JdbcTemplate jdbc;

    public PgPushRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void retire(UUID notice) {
        jdbc.update("UPDATE notifications SET status='skipped' WHERE id=? AND channel='push' AND status IN ('pending','failed')", notice);
    }

    public void recordDecision(String user, String alert, String decision) {
        jdbc.update("UPDATE notifications SET data=jsonb_set(data,'{decision}',to_jsonb(?::text)) WHERE user_id=? AND data->>'alertId'=?", decision, user, alert);
    }
}
