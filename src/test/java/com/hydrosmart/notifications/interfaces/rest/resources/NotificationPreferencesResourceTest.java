package com.hydrosmart.notifications.interfaces.rest.resources;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NotificationPreferencesResourceTest {
 Map<String,Object> valid(){return new HashMap<>(Map.of("emailEnabled",true,"inAppEnabled",true,"highConsumptionEnabled",true,"leakAlertsEnabled",true,"goalEnabled",true,"summariesEnabled",false));}
 @Test void validPreferencesKeepTheirWireNames(){var values=valid();assertEquals(values,NotificationPreferencesResource.from(values).values());}
 @Test void missingUnknownAndNullPreferencesAreRejected(){
  var missing=valid();missing.remove("goalEnabled");assertThrows(IllegalArgumentException.class,()->NotificationPreferencesResource.from(missing));
  var unknown=valid();unknown.put("extra",true);assertThrows(IllegalArgumentException.class,()->NotificationPreferencesResource.from(unknown));
  var nil=valid();nil.put("emailEnabled",null);assertThrows(IllegalArgumentException.class,()->NotificationPreferencesResource.from(nil));
 }
}
