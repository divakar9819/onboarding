package com.onboarding.onboarding.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.validation.Validator;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 12:49 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Configuration
public class ConfigBean {

    @Value("${spring.auth.service.baseurl}")
    private String authServiceBaseurl;

    @Value("${spring.card.service.baseurl}")
    private String cardServiceBaseurl;

    @Value("${spring.tm.service.baseurl}")
    private String tmServiceBaseurl;

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    public WebClient getAuthWebClient(){
        return createWebClient(authServiceBaseurl);
    }

    @Bean
    public WebClient getCardWebClient(){
        return createWebClient(cardServiceBaseurl);
    }

    @Bean
    public WebClient getTMWebClient(){
        return createWebClient(tmServiceBaseurl);
    }

    public WebClient createWebClient(String baseurl){
        return WebClient.builder().baseUrl(baseurl).build();
    }


}
