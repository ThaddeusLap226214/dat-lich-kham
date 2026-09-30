package com.clinic.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReminderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderServiceApplication.class, args);
    }
}
