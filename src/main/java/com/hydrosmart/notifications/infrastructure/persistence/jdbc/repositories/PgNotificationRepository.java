package com.hydrosmart.notifications.infrastructure.persistence.jdbc.repositories;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.hydrosmart.notifications.domain.services.NotificationRepository;
import com.hydrosmart.notifications.domain.model.aggregates.Notice;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;
@Repository
public class PgNotificationRepository implements NotificationRepository {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;
 public PgNotificationRepository(JdbcTemplate jdbc,org.springframework.transaction.PlatformTransactionManager manager){this.jdbc=jdbc;tx=new TransactionTemplate(manager);}
 public Map<String,Object> preferences(String user){
  var rows=jdbc.queryForList("SELECT * FROM notification_preferences WHERE user_id=?",user);
  var values=new HashMap<String,Object>();
  values.put("emailEnabled",true);values.put("inAppEnabled",true);values.put("highConsumptionEnabled",true);values.put("leakAlertsEnabled",true);values.put("goalEnabled",true);values.put("summariesEnabled",false);
  if(!rows.isEmpty()){var row=rows.getFirst();values.put("emailEnabled",row.get("email_enabled"));values.put("inAppEnabled",row.get("in_app_enabled"));values.put("highConsumptionEnabled",row.get("high_consumption_enabled"));values.put("leakAlertsEnabled",row.get("leak_alerts_enabled"));values.put("goalEnabled",row.get("goal_enabled"));values.put("summariesEnabled",row.get("summaries_enabled"));}
  return values;
 }
 public void preferences(String user,Map<String,Boolean> v){
  jdbc.update("INSERT INTO notification_preferences(user_id,email_enabled,in_app_enabled,high_consumption_enabled,leak_alerts_enabled,goal_enabled,summaries_enabled) VALUES(?,?,?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET email_enabled=excluded.email_enabled,in_app_enabled=excluded.in_app_enabled,high_consumption_enabled=excluded.high_consumption_enabled,leak_alerts_enabled=excluded.leak_alerts_enabled,goal_enabled=excluded.goal_enabled,summaries_enabled=excluded.summaries_enabled,updated_at=now()",user,v.get("emailEnabled"),v.get("inAppEnabled"),v.get("highConsumptionEnabled"),v.get("leakAlertsEnabled"),v.get("goalEnabled"),v.get("summariesEnabled"));
 }
 public void receive(Notice notice){
  tx.executeWithoutResult(status->{
   var inserted=jdbc.queryForList("INSERT INTO processed_events(event_id) VALUES(?) ON CONFLICT DO NOTHING RETURNING event_id",notice.source());if(inserted.isEmpty())return;
   var p=preferences(notice.user());
   if("password_reset".equals(notice.kind())){
    UUID id=UUID.randomUUID();
    jdbc.update("INSERT INTO notifications(id,user_id,source_event_id,channel,title,body,kind,data) VALUES(?,?,?,'email',?,?,?,?::jsonb)",id,notice.user(),notice.source(),notice.title(),notice.body(),notice.kind(),RpcBus.JSON.writeValueAsString(notice.data()));
    enqueue(id,notice.user());return;
   }
   if(("high_consumption".equals(notice.kind())&&!Boolean.TRUE.equals(p.get("highConsumptionEnabled")))||
      ("possible_leak".equals(notice.kind())&&!Boolean.TRUE.equals(p.get("leakAlertsEnabled")))||
      ("goal_80".equals(notice.kind())&&!Boolean.TRUE.equals(p.get("goalEnabled"))))return;
   for(String channel:List.of("in_app","email","push")){
    if(channel.equals("push")&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM push_tokens WHERE user_id=? AND revoked_at IS NULL AND p256dh IS NOT NULL)",Boolean.class,notice.user())))continue;
    if(channel.equals("in_app")&&!Boolean.TRUE.equals(p.get("inAppEnabled")))continue;
    if(channel.equals("email")&&(!Boolean.TRUE.equals(p.get("emailEnabled"))||("report".equals(notice.kind())&&!Boolean.TRUE.equals(p.get("summariesEnabled")))))continue;
    UUID id=UUID.randomUUID();
    jdbc.update("INSERT INTO notifications(id,user_id,source_event_id,channel,title,body,kind,status,sent_at,data) VALUES(?,?,?,?,?,?,?, ?,CASE WHEN ?='in_app' THEN now() ELSE NULL END,?::jsonb)",id,notice.user(),notice.source(),channel,notice.title(),notice.body(),notice.kind(),channel.equals("in_app")?"sent":"pending",channel,RpcBus.JSON.writeValueAsString(notice.data()));
    if(channel.equals("email"))enqueue(id,notice.user());
    if(channel.equals("push")){
     UUID event=UUID.randomUUID();var payload=Map.of("event_id",event.toString(),"event_type","notifications.push.requested.v1","producer","notifications","schema_version",1,"occurred_at",Instant.now().toString(),"data",Map.of("notification_id",id.toString(),"user_id",notice.user()));
     jdbc.update("INSERT INTO outbox_events(event_id,event_type,aggregate_id,payload) VALUES(?,'notifications.push.requested.v1',?,?::jsonb)",event,notice.user(),RpcBus.JSON.writeValueAsString(payload));
    }
   }
  });
 }
 private void enqueue(UUID id,String user){
  UUID event=UUID.randomUUID();
  var payload=Map.of("event_id",event.toString(),"event_type","notifications.email.requested.v1","producer","notifications","schema_version",1,"occurred_at",Instant.now().toString(),"data",Map.of("notification_id",id.toString(),"user_id",user));
  jdbc.update("INSERT INTO outbox_events(event_id,event_type,aggregate_id,payload) VALUES(?,'notifications.email.requested.v1',?,?::jsonb)",event,user,RpcBus.JSON.writeValueAsString(payload));
 }
 public Map<String,Object> list(String user,UUID before){
  String select="SELECT id,title,body,kind,created_at,read_at,data FROM notifications WHERE user_id=? AND channel='in_app'";
  var rows=before==null?jdbc.queryForList(select+" ORDER BY created_at DESC,id DESC LIMIT 101",user):jdbc.queryForList(select+" AND (created_at,id)<(SELECT created_at,id FROM notifications WHERE id=? AND user_id=? AND channel='in_app') ORDER BY created_at DESC,id DESC LIMIT 101",user,before,user);
  boolean more=rows.size()>100;var items=new ArrayList<>(rows.subList(0,Math.min(100,rows.size())));
  for(var item:items)for(String field:List.of("created_at","read_at"))if(item.get(field) instanceof Timestamp value)item.put(field,value.toInstant().toString());
  items.forEach(this::metadata);
  long unread=jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id=? AND channel='in_app' AND read_at IS NULL",Long.class,user);
  var email=jdbc.queryForMap("SELECT count(*) FILTER(WHERE status='pending') AS pending,count(*) FILTER(WHERE status='failed') AS failed,count(*) FILTER(WHERE status='sent') AS accepted FROM notifications WHERE user_id=? AND channel='email'",user);
  var result=new HashMap<String,Object>();result.put("items",items);result.put("unreadCount",unread);result.put("email",email);
  result.put("push",jdbc.queryForMap("SELECT count(*) FILTER(WHERE status='pending') AS pending,count(*) FILTER(WHERE status='failed') AS failed,count(*) FILTER(WHERE status='sent') AS accepted FROM notifications WHERE user_id=? AND channel='push'",user));
  result.put("nextCursor",more?items.getLast().get("id").toString():null);return result;
 }
 public boolean read(String user,UUID id){return jdbc.update("UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE user_id=? AND id=? AND channel='in_app'",user,id)>0;}
 private void metadata(Map<String,Object> row){if(row.containsKey("data"))row.put("data",RpcBus.JSON.readValue(row.get("data").toString(),Map.class));}
 public Map<String,Object> email(UUID id){var result=jdbc.queryForList("SELECT * FROM notifications WHERE id=? AND channel='email'",id).stream().findFirst().orElse(Map.of());if(!result.isEmpty())metadata(result);return result;}
 public void sent(UUID id,String providerId){jdbc.update("UPDATE notifications SET status='sent',sent_at=now(),provider_message_id=?,last_error=NULL,data=CASE WHEN kind='password_reset' THEN '{}'::jsonb ELSE data END WHERE id=? AND channel='email'",providerId,id);}
 public void failed(UUID id,String reason){jdbc.update("UPDATE notifications SET attempts=attempts+1,last_error=?,status=CASE WHEN attempts>=5 THEN 'failed' ELSE 'pending' END WHERE id=? AND channel='email'",reason,id);}
 public void expired(UUID id){jdbc.update("UPDATE notifications SET status='failed',data='{}'::jsonb,last_error='El enlace venció. Solicita uno nuevo desde Iniciar sesión.' WHERE id=? AND kind='password_reset'",id);}
 public void retry(String user){tx.executeWithoutResult(status->{for(var row:jdbc.queryForList("UPDATE notifications SET status='pending',attempts=0,last_error=NULL WHERE user_id=? AND channel='email' AND status='failed' AND kind!='password_reset' RETURNING id",user))enqueue((UUID)row.get("id"),user);});}
}

