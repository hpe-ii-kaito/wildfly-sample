package com.example.wildfly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingResourceTest {

    private final GreetingResource resource = new GreetingResource(new GreetingService());

    @Test
    void returnsAPersonalizedGreeting() {
        assertEquals("Hello, WildFly!", resource.greet("WildFly").getMessage());
    }

    @Test
    void returnsADefaultGreeting() {
        assertEquals("Hello, World!", resource.greet(null).getMessage());
    }
}
