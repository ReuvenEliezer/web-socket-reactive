package com.reuven.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAdminServer
@SpringBootApplication
public class AdminApplication {

    //https://docs.spring-boot-admin.com/current/getting-started.html
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }

}
