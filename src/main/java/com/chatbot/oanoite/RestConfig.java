package com.chatbot.oanoite;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // Diz ao Spring que esta classe contém configurações
public class RestConfig {

    @Bean // Diz ao Spring que este método é a "receita" para um Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Este método constrói e retorna o objeto RestTemplate
        return builder.build();
    }
}