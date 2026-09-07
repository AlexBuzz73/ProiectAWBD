package com.example.accountservice.repositories;

import com.example.accountservice.domain.AccountAccess;
import com.example.accountservice.dto.AccountCurrencySummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountAccessRepository extends JpaRepository<AccountAccess, Long> {

    List<AccountAccess> findByUserIdAndStatus(Integer userId, String status);

    Optional<AccountAccess> findByAccountAccountIdAndUserIdAndStatus(Long accountId, Integer userId, String status);

    Optional<AccountAccess> findByAccountAccountIdAndUserId(Long accountId, Integer userId);

    List<AccountAccess> findByAccountAccountIdAndStatus(Long accountId, String status);

    List<AccountAccess> findByAccountAccountId(Long accountId);

    boolean existsByAccountAccountIdAndUserId(Long accountId, Integer userId);

    boolean existsByAccountAccountIdAndUserIdAndAccessRoleIn(Long accountId, Integer userId, List<String> roles);

    @Query("SELECT a FROM AccountAccess a WHERE a.userId = :userId AND a.status = 'ACTIVE' AND a.account.status = 'ACTIVE'")
    Page<AccountAccess> findActiveAccountsForUser(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT new com.example.accountservice.dto.AccountCurrencySummaryDTO(a.account.currency, COUNT(a.account), SUM(a.account.balance)) " +
           "FROM AccountAccess a " +
           "WHERE a.userId = :userId AND a.status = 'ACTIVE' AND a.account.status = 'ACTIVE' " +
           "GROUP BY a.account.currency")
    List<AccountCurrencySummaryDTO> getCurrencySummaryForUser(@Param("userId") Integer userId);
}
