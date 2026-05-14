package org.phoenix.demo;

import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAzureMessaging
public class DddEdaDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DddEdaDemoApplication.class, args);
    }

}
