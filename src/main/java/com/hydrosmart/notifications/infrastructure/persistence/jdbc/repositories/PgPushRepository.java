package com.hydrosmart.notifications.infrastructure.persistence.jdbc.repositories;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.hydrosmart.notifications.domain.services.PushRepository;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
@Repository
public class PgPushRepository implements PushRepository {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;
 public PgPushRepository(JdbcTemplate jdbc,org.springframework.transaction.PlatformTransactionManager manager){this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);}
 public void subscribe(String user,String endpoint,String p256dh,String auth){tx.executeWithoutResult(status->{
  jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,79))",Object.class,user);
  jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,83))",Object.class,endpoint);
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM push_tokens WHERE token=? AND user_id<>? AND revoked_at IS NULL)",Boolean.class,endpoint,user)))throw new IllegalArgumentException("Desactiva y vuelve a activar los avisos en este navegador.");
  if(jdbc.queryForObject("SELECT count(*) FROM push_tokens WHERE user_id=? AND revoked_at IS NULL AND token<>?",Integer.class,user,endpoint)>=10)throw new IllegalArgumentException("Puedes activar hasta diez navegadores. Desactiva uno para continuar.");
  jdbc.update("INSERT INTO push_tokens(id,user_id,token,p256dh,auth_secret) VALUES(?,?,?,?,?) ON CONFLICT(token) DO UPDATE SET user_id=excluded.user_id,p256dh=excluded.p256dh,auth_secret=excluded.auth_secret,revoked_at=NULL",UUID.randomUUID(),user,endpoint,p256dh,auth);
 });}
 public void unsubscribe(String user,String endpoint){jdbc.update("UPDATE push_tokens SET revoked_at=now() WHERE user_id=? AND token=?",user,endpoint);}
 public Map<String,Object> notice(UUID id){var row=jdbc.queryForList("SELECT * FROM notifications WHERE id=? AND channel='push'",id).stream().findFirst().orElse(Map.of());if(!row.isEmpty())row.put("data",RpcBus.JSON.readValue(row.get("data").toString(),Map.class));return row;}
 public List<Map<String,Object>> pending(UUID notice,String user){return jdbc.queryForList("SELECT p.* FROM push_tokens p WHERE user_id=? AND revoked_at IS NULL AND p256dh IS NOT NULL AND NOT EXISTS(SELECT 1 FROM push_deliveries d WHERE d.notification_id=? AND d.token_id=p.id)",user,notice);}
 public void delivered(UUID notice,UUID subscription){jdbc.update("INSERT INTO push_deliveries(notification_id,token_id) VALUES(?,?) ON CONFLICT DO NOTHING",notice,subscription);}
 public boolean hasDelivery(UUID notice){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM push_deliveries WHERE notification_id=?)",Boolean.class,notice));}
 public void failed(UUID notice){jdbc.update("UPDATE notifications SET attempts=attempts+1,status=CASE WHEN attempts+1>=6 THEN 'failed' ELSE 'pending' END,last_error='No se pudo entregar el aviso al servicio push.' WHERE id=? AND channel='push'",notice);}
 public void revoke(UUID subscription){jdbc.update("UPDATE push_tokens SET revoked_at=now() WHERE id=?",subscription);}
 public void finish(UUID notice,boolean success){jdbc.update("UPDATE notifications SET status=?,sent_at=CASE WHEN ? THEN now() ELSE NULL END WHERE id=? AND channel='push'",success?"sent":"failed",success,notice);}
 public void recordDecision(String user,String alert,String decision){jdbc.update("UPDATE notifications SET data=jsonb_set(data,'{decision}',to_jsonb(?::text)) WHERE user_id=? AND data->>'alertId'=?",decision,user,alert);}
 public void fallback(UUID notice){tx.executeWithoutResult(status->{
  var rows=jdbc.queryForList("INSERT INTO notifications(id,user_id,source_event_id,channel,title,body,kind,data) SELECT ?,n.user_id,n.source_event_id,'email',n.title,n.body,n.kind,n.data FROM notifications n LEFT JOIN notification_preferences p ON p.user_id=n.user_id WHERE n.id=? AND COALESCE(p.email_enabled,true) ON CONFLICT(user_id,source_event_id,channel) DO NOTHING RETURNING id,user_id",UUID.randomUUID(),notice);
  for(var row:rows){UUID event=UUID.randomUUID();var payload=Map.of("event_id",event.toString(),"event_type","notifications.email.requested.v1","schema_version",1,"producer","notifications","occurred_at",Instant.now().toString(),"data",Map.of("notification_id",row.get("id").toString(),"user_id",row.get("user_id")));
   jdbc.update("INSERT INTO outbox_events(event_id,event_type,aggregate_id,payload) VALUES(?,'notifications.email.requested.v1',?,?::jsonb)",event,row.get("user_id"),RpcBus.JSON.writeValueAsString(payload));}
 });}
}
