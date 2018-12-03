package com.calipsoide.diffing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.SpringApplication.run;

/**
 * Main class for running the application.
 * <p>
 * System is built on top of Spring Boot, and uses an embedded MongoDB to make things simpler.
 */
@SpringBootApplication
public class DiffingApplication {

    public static void main(String[] args) {
        run(DiffingApplication.class, args);
    }

}
