package com.chatbot.oanoite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleApiTest implements CommandLineRunner {
    
    @Autowired
    private RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n\n--- INICIANDO TESTE DE API DO GOOGLE ---");
        
        // Este é o URL que o erro sugeriu para listar os modelos
        String url = "https://generativelanguage.googleapis.com/v1/models?key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, null, String.class);
            
            System.out.println("--- SUCESSO! Modelos disponíveis: ---");
            System.out.println(response.getBody());
            System.out.println("-----------------------------------------");
            
        } catch (Exception e) {
            System.err.println("--- ERRO AO LISTAR MODELOS ---");
            System.err.println(e.getMessage());
            System.err.println("--------------------------------");
        }
        
        System.out.println("\n\n");
    }
}