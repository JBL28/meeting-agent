package com.ssafy.meeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MeetingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingAgentApplication.class, args);
    }
}
