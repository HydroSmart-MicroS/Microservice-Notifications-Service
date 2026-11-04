package com.hydrosmart.notifications;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class Application {
    public static void main(String[] args) { com.hydrosmart.notifications.shared.infrastructure.configuration.RemoteConfiguration.load(); SpringApplication.run(Application.class, args); }
}
