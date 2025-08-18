package com.mansurtiyes.newportwhaleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewportWhaleApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewportWhaleApiApplication.class, args);
    }

}
