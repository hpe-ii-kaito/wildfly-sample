package com.example.wildfly;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public Greeting greet(String name) {
        String recipient = name == null || name.isBlank() ? "World" : name.strip();
        return new Greeting("Hello, " + recipient + "!");
    }
}
