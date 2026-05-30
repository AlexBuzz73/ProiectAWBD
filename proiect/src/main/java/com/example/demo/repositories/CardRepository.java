package com.example.demo.repositories;

import com.example.demo.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findCardByAccountAccountId(Long accountId);
    Boolean existsCardByAccountAccountId(Long accountId);
    Boolean existsCardByCardNumber(String cardNumber);
}
