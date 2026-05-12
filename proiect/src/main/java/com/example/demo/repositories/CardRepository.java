package com.example.demo.repositories;

import com.example.demo.domain.Card;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findCardByAccountAccountIdAndStatus(Long accountId, String status);
    Boolean existsCardByAccountAccountIdAndStatus(Long accountId, String status);
    Boolean existsCardByCardNumber(String cardNumber);
}
