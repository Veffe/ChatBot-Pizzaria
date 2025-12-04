package com.chatbot.oanoite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    public String classificarIntencao(String textoUsuario, String estadoAtual) {
        
        String estado = (estadoAtual == null || estadoAtual.isEmpty()) ? "padrao" : estadoAtual;
        
        // URL da API do Gemini (corrigido)
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        // --- Prompt atualizado ---
        String prompt = criarPromptSimples(textoUsuario, estado);
        
        Map<String, String> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map<Object, Object>> candidates = (List<Map<Object, Object>>) responseBody.get("candidates");
                Map<Object, Object> contentResponse = (Map<Object, Object>) candidates.get(0).get("content");
                List<Map<Object, Object>> parts = (List<Map<Object, Object>>) contentResponse.get("parts");
                String intentId = (String) parts.get(0).get("text");

                intentId = intentId.trim();
                log.info("LLM (via Rest) Classificou: {} -> {}", textoUsuario, intentId);
                return intentId;
            } else {
                log.warn("Resposta da LLM veio vazia ou mal formatada.");
                return "NAO_ENTENDEU";
            }

        } catch (Exception e) {
            log.error("Erro ao chamar a API do Gemini via RestTemplate: {}", e.getMessage());
            return "NAO_ENTENDEU";
        }
    }

    // --- MÉTODO ATUALIZADO ---
    private String criarPromptSimples(String textoUsuario, String estado) {
        return "Classifique o 'Texto do Usuário' baseado no 'Estado Atual'. Responda APENAS com o ID da intenção.\n" +
               "\n" +
               "--- ESTADO ATUAL ---\n" +
               estado + "\n" +
               "\n" +
               "--- TEXTO DO USUÁRIO ---\n" +
               "\"" + textoUsuario + "\"\n" +
               "\n" +
               "--- INTENÇÕES POSSÍVEIS ---\n" +
               "Se o estado for 'AGUARDANDO_ESCOLHA_SABOR' -> ESCOLHEU_SABOR\n" +
               "Se o estado for 'AGUARDANDO_TAMANHO' -> ESCOLHEU_TAMANHO\n" +
               "Se o estado for 'AGUARDANDO_BEBIDA' -> ESCOLHEU_BEBIDA\n" +
               "Se o estado for 'AGUARDANDO_PAGAMENTO' -> ESCOLHEU_PAGAMENTO\n" +
               "Se o estado for 'AGUARDANDO_CEP' ou for '1'(e o texto for um CEP) -> ENVIOU_CEP\n" +
               "Se o estado for 'ENVIOU_CEP_VERIFICACAO' ou for '1'(e o texto for um CEP) -> ENVIOU_CEP_VERIFICACAO\n" +
               "Se o estado for 'AGUARDANDO_NUMERO_CASA' (e o texto for um número) -> ENVIOU_NUMERO_CASA\n" +
               "Se o estado for 'AGUARDANDO_COMPLEMENTO' -> ENVIOU_COMPLEMENTO\n" +
               "Se o texto for 'oi' ou 'olá' ou qualquer tipo de saudação brasileira -> SAUDACAO\n" +
               "Se o texto for 'menu' ou 'cardápio' ou qualquer coisa que remeta 'sabores' ou '2'-> VER_CARDAPIO\n" +
               "Se o texto perguntar onde entrega ou '1'-> VER_LOCAIS\n" +
               "Se o texto pedir ajuda ou atendente ou '4'-> FALAR_ATENDENTE\n" +
               "Se o texto for 'pedir' ou 'quero uma pizza' ou '3' -> INICIAR_PEDIDO\n" +
               "Se o texto for 'tchau' ou 'até logo' ou qualquer despedida brasileira -> DESPEDIDA\n" +
               "Se não for nada disso -> NAO_ENTENDEU\n" +
               "\n" +
               "ID DA INTENÇÃO:";
    }
}