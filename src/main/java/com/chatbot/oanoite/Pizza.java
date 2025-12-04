package com.chatbot.oanoite;

// Importações do JPA (Jakarta Persistence API)
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity 
@Table(name = "pizzas") 
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    private String nome;
    private String descricao; 
    private double preco; // <-- Este é o PREÇO BASE (ex: Média)
    
    // Construtor vazio
    public Pizza() { }

    // Getters e Setters simples
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    
    public double getPreco() { return preco; }
    
    // O setPreco agora é "burro". Ele só salva o valor.
    public void setPreco(double preco) {
        this.preco = preco;
    }
}