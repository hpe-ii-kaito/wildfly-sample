package com.example.wildfly;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @ParameterizedTest
    @CsvSource({
            "Alice, 'Hello, Alice!'",
            "WildFly, 'Hello, WildFly!'",
            "'  Alice  ', 'Hello, Alice!'",
            "'Mary Jane', 'Hello, Mary Jane!'"
    })
    void greetsTheNamedRecipient(String name, String expected) {
        assertEquals(expected, service.greet(name).getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void usesWorldForMissingOrBlankNames(String name) {
        assertEquals("Hello, World!", service.greet(name).getMessage());
    }
}
