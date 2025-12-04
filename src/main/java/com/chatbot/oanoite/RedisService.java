package com.chatbot.oanoite;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // O estado (o que o bot espera) expira em 15 minutos
    private static final long TIMEOUT_ESTADO_MINUTOS = 10;
    // O carrinho (o que o usuário escolheu) expira em 20 minutos
    private static final long TIMEOUT_CARRINHO_MINUTOS = 5;


    // --- MÉTODOS DE ESTADO (Onde o usuário está) ---

    public void setEstado(String remetente, String estado) {
        String key = remetente + ":estado"; // Chave principal do estado
        if (estado == null || estado.isEmpty()) {
            redisTemplate.delete(key);
        } else {
            // Usa o timeout de 15 minutos
            redisTemplate.opsForValue().set(key, estado, TIMEOUT_ESTADO_MINUTOS, TimeUnit.MINUTES);
        }
    }

    public String getEstado(String remetente) {
        return redisTemplate.opsForValue().get(remetente + ":estado");
    }

    // --- MÉTODOS DE VALOR (O "Carrinho de Compras") ---

    public void setValor(String remetente, String campo, String valor) {
        String chave = remetente + ":" + campo; // Ex: "5511...:pedido_sabor"
        // Usa o timeout de 20 minutos
        redisTemplate.opsForValue().set(chave, valor, TIMEOUT_CARRINHO_MINUTOS, TimeUnit.MINUTES);
    }

    public String getValor(String remetente, String campo) {
        String chave = remetente + ":" + campo;
        return redisTemplate.opsForValue().get(chave);
    }

    /**
     * Limpa todo o pedido (estado e valores) do usuário.
     */
    public void limparPedido(String remetente) {
        // Encontra todas as chaves relacionadas a este remetente
        Set<String> chaves = redisTemplate.keys(remetente + ":*");
        if (chaves != null && !chaves.isEmpty()) {
            redisTemplate.delete(chaves);
        }
    }
}