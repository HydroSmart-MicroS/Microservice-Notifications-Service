package com.hydrosmart.notifications.interfaces.rest.resources;
import java.util.Map;
import java.util.Set;
public record NotificationPreferencesResource(boolean emailEnabled,boolean inAppEnabled,boolean highConsumptionEnabled,boolean leakAlertsEnabled,boolean goalEnabled,boolean summariesEnabled) {
 public static NotificationPreferencesResource from(Map<String,?> values){
  if(!values.keySet().equals(Set.of("emailEnabled","inAppEnabled","highConsumptionEnabled","leakAlertsEnabled","goalEnabled","summariesEnabled"))||values.values().stream().anyMatch(value->!(value instanceof Boolean)))throw new IllegalArgumentException("Envía las seis preferencias como valores booleanos.");
  return new NotificationPreferencesResource((Boolean)values.get("emailEnabled"),(Boolean)values.get("inAppEnabled"),(Boolean)values.get("highConsumptionEnabled"),(Boolean)values.get("leakAlertsEnabled"),(Boolean)values.get("goalEnabled"),(Boolean)values.get("summariesEnabled"));
 }
 public Map<String,Boolean> values(){return Map.of("emailEnabled",emailEnabled,"inAppEnabled",inAppEnabled,"highConsumptionEnabled",highConsumptionEnabled,"leakAlertsEnabled",leakAlertsEnabled,"goalEnabled",goalEnabled,"summariesEnabled",summariesEnabled);}
}
