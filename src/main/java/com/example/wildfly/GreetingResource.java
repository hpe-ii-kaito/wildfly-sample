package com.example.wildfly;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class GreetingResource {

    @Inject
    private GreetingService greetingService;

    public GreetingResource() {
    }

    GreetingResource(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GET
    public Greeting greet(@QueryParam("name") @DefaultValue("World") String name) {
        return greetingService.greet(name);
    }
}
