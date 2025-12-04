package com.chatbot.oanoite;

// --- Imports Corrigidos ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
// Importa o "molde" que você criou


@Service
public class ConsultaEnd {

    // --- CORREÇÃO 1: Definindo o Logger ---
    private static final Logger log = LoggerFactory.getLogger(ConsultaEnd.class);

    // --- CORREÇÃO 2: Injetando a ferramenta, não a receita ---
    @Autowired 
    private RestTemplate restTemplate;

    public EnderecoResponse consultarCep(String cep) {
        String cepLimpo = cep.replaceAll("[^0-9]", "");
        
        if (cepLimpo.length() != 8) {
            log.warn("CEP inválido (não tem 8 dígitos): {}", cep);
            return null;
        }

        String url = "https://viacep.com.br/ws/" + cepLimpo + "/json/";
        log.info("Consultando API ViaCEP: {}", url);

        try {
            // --- CORREÇÃO 3: Usando 'restTemplate' ---
            EnderecoResponse endereco = restTemplate.getForObject(url, EnderecoResponse.class);

            // O ViaCEP retorna um JSON com "erro: true" se o CEP não existe
            if (endereco != null && endereco.getCep() == null) {
                log.warn("ViaCEP retornou um CEP válido, mas não encontrado.");
                return null;
            }
            
            log.info("Endereço encontrado: {}, {}", endereco.getLogradouro(), endereco.getBairro());
            return endereco;

        } catch (Exception e) {
            log.error("Erro ao consultar ViaCEP: {}", e.getMessage());
            return null; // Retorna nulo se a API falhar
        }
    }
    
}