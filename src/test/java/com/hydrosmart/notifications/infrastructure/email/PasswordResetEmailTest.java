package com.hydrosmart.notifications.infrastructure.email;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordResetEmailTest {
 @Test void rendersRecoveryButtonWithoutAcceptingAnotherOrigin(){
  String link="http://localhost:8080/api/auth/reset-password/test-one-use?callbackURL=http%3A%2F%2Flocalhost%3A8080%2Fapp%2Freset-password";
  var result=NotificationEmailTemplate.render("password_reset","Recupera tu cuenta","Solo tú & nadie más","http://localhost:8080",Map.of("actionUrl",link));
  assertTrue(result.html().contains("Elegir nueva contraseña"));assertTrue(result.html().contains("Solo tú &amp; nadie más"));assertTrue(result.text().contains(link));
  assertThrows(IllegalArgumentException.class,()->NotificationEmailTemplate.render("password_reset","Reset","body","http://localhost:8080",Map.of("actionUrl","https://evil.example/api/auth/reset-password/token")));
 }
}
