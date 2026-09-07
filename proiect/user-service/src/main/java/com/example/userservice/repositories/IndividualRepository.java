package com.example.userservice.repositories;

import com.example.userservice.domain.Individual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndividualRepository extends JpaRepository<Individual, Integer> {
    Optional<Individual> findByCnp(String cnp);
    boolean existsByCnp(String cnp);
}
