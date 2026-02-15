package ru.akutepov.exchangeratesbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        Server testServer = new Server();
        Server localServer = new Server();
        testServer.setUrl("http://104.248.39.82:8080");
        localServer.setUrl("http://127.0.0.1:8080");

        return new OpenAPI()
                .addServersItem(testServer)
                .addServersItem(localServer);
    }
}

