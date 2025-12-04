package com.chatbot.oanoite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BebidaRepository extends JpaRepository<Bebida, Long> {

    // Encontra uma bebida pelo nome, ignorando maiúsculas/minúsculas
    Optional<Bebida> findByNomeContainingIgnoreCase(String nome);
}