package com.hydrosmart.notifications.infrastructure.email;

import java.net.URI;

public final class NotificationEmailTemplate {
 private NotificationEmailTemplate(){}
 public record Content(String html,String text){}
 private record Theme(String label,String color,String background,String hint,String action,String path){}
 public static Content render(String kind,String title,String body,String publicUrl){
  var base=URI.create(publicUrl);
  if((!"http".equalsIgnoreCase(base.getScheme())&&!"https".equalsIgnoreCase(base.getScheme()))||base.getHost()==null||base.getRawUserInfo()!=null||base.getRawQuery()!=null||base.getRawFragment()!=null)
   throw new IllegalArgumentException("PUBLIC_APP_URL debe ser una dirección HTTP/HTTPS válida de HydroSmart.");
  String origin=publicUrl.replaceAll("/+$","");
  var theme=switch(kind){
   case "high_consumption"->new Theme("CONSUMO INUSUAL","#8a560a","#fff5df","Revisa qué medidor concentró el consumo y compáralo con tus hábitos recientes.","Revisar mis alertas","/alertas");
   case "possible_leak"->new Theme("POSIBLE FUGA","#a13d2c","#fff0eb","Revisa las llaves y conexiones de la unidad. El flujo continuo es una señal para revisar, no una confirmación de fuga.","Revisar posible fuga","/alertas");
   case "goal_80"->new Theme("PRESUPUESTO MENSUAL","#8a560a","#fff5df","Revisa el avance de tu presupuesto y planifica tu consumo para el resto del mes.","Ver mi meta mensual","/alertas");
   case "water_payment_pending"->new Theme("PLAN Y PAGOS","#8a560a","#fff5df","El estado puede haber cambiado desde este aviso. Compruébalo en Historial de pagos antes de pagar.","Ir a Plan y pagos","/pagos");
   case "payment"->new Theme("PAGO CONFIRMADO","#126957","#eaf5ef","Puedes consultar este pago y el estado de tu cuenta desde Plan y pagos.","Ver mis pagos","/pagos");
   case "report"->new Theme("REPORTE DISPONIBLE","#245e79","#edf5fa","Abre tu historial de consumo para consultar y descargar el reporte disponible.","Ver Historial","/historial");
   default->new Theme("TU CUENTA","#126957","#eaf5ef","Encontrarás los detalles de este aviso en tu bandeja de notificaciones.","Ver notificaciones","/notificaciones");
  };
  String actionUrl=origin+"/app"+theme.path(),preferencesUrl=origin+"/app/notificaciones";
  String preview=body.length()>150?body.substring(0,147)+"…":body;
  String html="""
<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="color-scheme" content="light"><title>%s</title></head>
<body style="margin:0;padding:0;background-color:#f2f6f4;font-family:Arial,Helvetica,sans-serif;color:#193d40;-webkit-text-size-adjust:100%%;">
<div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;color:#f2f6f4;">%s</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" bgcolor="#f2f6f4"><tr><td align="center" style="padding:28px 12px;">
<!--[if mso]><table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0"><tr><td><![endif]-->
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%;max-width:600px;background-color:#ffffff;border:1px solid #dce7e1;border-radius:16px;overflow:hidden;">
<tr><td bgcolor="#103f43" style="padding:28px;color:#ffffff;">
 <p style="margin:0;font-size:21px;line-height:28px;font-weight:bold;letter-spacing:2px;color:#ffffff;">HYDROSMART</p>
 <p style="margin:7px 0 0;font-size:13px;line-height:20px;color:#c7e3d9;">Tu agua, bajo control.</p>
</td></tr>
<tr><td style="padding:30px 28px 28px;">
 <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr><td bgcolor="%s" style="padding:7px 10px;border-radius:5px;color:%s;font-size:10px;line-height:16px;font-weight:bold;letter-spacing:1.2px;">%s</td></tr></table>
 <h1 style="margin:18px 0 16px;font-size:27px;line-height:35px;font-weight:bold;color:#193d40;">%s</h1>
 <p style="margin:0 0 24px;font-size:16px;line-height:26px;color:#405b5e;overflow-wrap:anywhere;">%s</p>
 <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" bgcolor="%s" style="border-radius:8px;"><tr><td style="padding:17px 18px;">
  <p style="margin:0 0 6px;font-size:11px;line-height:17px;font-weight:bold;letter-spacing:0.8px;color:#244d50;">QUÉ PUEDES HACER</p>
  <p style="margin:0;font-size:14px;line-height:22px;color:#405b5e;">%s</p>
 </td></tr></table>
 <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin-top:26px;"><tr><td bgcolor="#087e75" style="border-radius:7px;mso-padding-alt:14px 20px;text-align:center;">
  <a href="%s" style="display:inline-block;padding:14px 20px;font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:20px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:7px;">%s</a>
 </td></tr></table>
</td></tr>
<tr><td style="padding:22px 28px;border-top:1px solid #e6ede8;background-color:#fbfcfb;">
 <p style="margin:0 0 9px;font-size:12px;line-height:19px;color:#526b6d;">Recibes este mensaje por la actividad de tu cuenta en HydroSmart.</p>
 <a href="%s" style="font-size:12px;line-height:19px;color:#087e75;text-decoration:underline;">Gestionar notificaciones</a>
</td></tr></table>
<!--[if mso]></td></tr></table><![endif]-->
<p style="max-width:540px;margin:18px 8px 0;font-size:11px;line-height:18px;color:#6c817d;">Demostración de HydroSmart · Los datos de consumo son simulados.</p>
</td></tr></table></body></html>
""".formatted(escape(title),escape(preview),theme.background(),theme.color(),theme.label(),escape(title),escape(body).replace("\r\n","\n").replace("\n","<br>"),theme.background(),escape(theme.hint()),escape(actionUrl),theme.action(),escape(preferencesUrl));
  String text=title+"\n\n"+body+"\n\n"+theme.hint()+"\n\n"+theme.action()+": "+actionUrl+"\n\nGestionar notificaciones: "+preferencesUrl+"\n\nHydroSmart · Los datos de consumo de esta demostración son simulados.";
  return new Content(html,text);
 }
 private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
