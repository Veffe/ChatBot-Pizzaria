package com.chatbot.oanoite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnderecoResponse {

    // Os nomes das variáveis DEVEM ser idênticos aos do JSON da ViaCEP
    private String cep;
    private String logradouro; // Rua
    private String bairro;
    private String localidade; // Cidade
    private String uf; // Estado

    // Construtor vazio (necessário para o Spring)
    public EnderecoResponse() {
    }
    // --- GETTERS E SETTERS ---
    // (O Spring usa isso para preencher o objeto com o JSON)

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }
    public String getLocalidade() {
        return localidade;
    }
    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }
    public String getUf() {
        return uf;
    }
    public void setUf(String uf) {
        this.uf = uf;
    }

    
}
