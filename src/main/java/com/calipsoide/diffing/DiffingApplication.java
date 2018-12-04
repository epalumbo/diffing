package com.calipsoide.diffing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.SpringApplication.run;

/**
 * Main class for running the application.
 * <p>
 * The system is built on top of Spring Boot. It uses an embedded MongoDB to make things simpler,
 * but it can be easily changed to use a productive instance of the database.
 * <p>
 * Given that scaling is a requirement, this service was developed using reactive programming.
 * Reactive applications can handle a huge amount of requests in an asynchronous, non-blocking way,
 * meaning that we can reach high throughput levels with a pool of a few threads.
 * That helps to keep the application responsive when it's under heavy load.
 * <p>
 * Spring 5 provides excellent support to create fully reactive applications on the JVM.
 * Spring Web Reactive framework enable us to set up a reactive REST API with very few lines of configuration code.
 * Project Reactor, which is Spring's Reactive Streams implementation, lets us write algorithms in a declarative,
 * functional style, which gives us elegant, compact and clean code.
 */
@SpringBootApplication
public class DiffingApplication {

    public static void main(String[] args) {
        run(DiffingApplication.class, args);
    }

}
