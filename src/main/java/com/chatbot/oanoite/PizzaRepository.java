package com.chatbot.oanoite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    // Você já deve ter este
    Optional<Pizza> findByNome(String nome);
    Optional<Pizza> findByNomeContainingIgnoreCase(String nome);
}