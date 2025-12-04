package com.chatbot.oanoite;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class Controller {
    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    // ...
    @Autowired
    private BotService botService; // (Convenção: nome da variável minúsculo)
    
    @PostMapping("/api")
    public ResponseEntity<Void> receberMensagemEvolution(@RequestBody Map<String, Object> payload) {
    
        log.info("Webhook da Evolution API recebido!");
        try {
            // --- CORREÇÃO ---
            // Agora você está enviando o payload para o "Cérebro"
            botService.processarMensagem(payload);

        } catch (Exception e) {
            log.error("Erro ao processar webhook da Evolution: ", e);
        }

        // Responde OK para a API saber que você recebeu
        return ResponseEntity.ok().build();
    }
}


