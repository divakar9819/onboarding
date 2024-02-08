package com.onboarding.onboarding.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * @author Divakar Verma
 * @created_at : 08/01/2024 - 12:39 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Configuration
public class OpenApiConfig {

    private String devUrl = "http://localhost:8086";
    private String prodUrl = "https://prod/url";

    @Bean
    public OpenAPI myOpenAPI(){
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server url in Development environment");

        Server uatServer = new Server();
        uatServer.setUrl(prodUrl);
        uatServer.setDescription("Server url in uat environment");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Server url in Production environment");

        Contact contact = new Contact();
        contact.setEmail("test@gmail.com");
        contact.setName("testuser");
        contact.setUrl("https://test/url");

        License mitLicense = new License().name("MIT Licence").url("https://choosealicense.com/licenses/mit/");
        Info info = new Info()
                .title("Onboarding")
                .version("1.0")
                .contact(contact)
                .description("This api exposes endpoint to manage Onboarding")
                .license(mitLicense);

        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList("Bearer Authentication");
        Components components = new Components();
        components.addSecuritySchemes("Bearer Authentication", createAPIKeyScheme());

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(info).servers(Arrays.asList(devServer,prodServer));

    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
