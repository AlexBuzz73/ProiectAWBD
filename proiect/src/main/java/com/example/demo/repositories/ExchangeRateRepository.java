package com.example.demo.repositories;

import com.example.demo.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Integer> {

    Optional<ExchangeRate> findByCurrencyFromAndCurrencyTo(String currencyFrom, String currencyTo);

    Optional<ExchangeRate> findTopByCurrencyFromAndCurrencyToOrderByRateDateDesc(String currencyFrom, String currencyTo);

    boolean existsByCurrencyFromAndCurrencyToAndRateDate(String currencyFrom, String currencyTo, Date rateDate);
}