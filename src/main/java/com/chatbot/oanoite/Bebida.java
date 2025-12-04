package com.chatbot.oanoite;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bebidas") // Nova tabela
public class Bebida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome; // Ex: "Coca-Cola 2L"
    private double preco; // Ex: 10.00
    private String litros;

    // Construtor vazio
    public Bebida() {
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public String getLitros(){return litros;}
    public void setLitros(String litros){this.litros = litros;}
}