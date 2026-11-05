package com.hydrosmart.notifications.domain.model.aggregates;
import java.util.UUID;
import java.util.Map;
public record Notice(UUID source,String user,String kind,String title,String body,Map<String,Object> data) {
 public Notice(UUID source,String user,String kind,String title,String body){this(source,user,kind,title,body,Map.of());}
 public Notice {
  if(source==null||user==null||user.isBlank()||user.length()>255||kind==null||title==null||body==null||title.length()>300||body.length()>8000)throw new IllegalArgumentException("Invalid notification");
  data=Map.copyOf(data);
 }
}

