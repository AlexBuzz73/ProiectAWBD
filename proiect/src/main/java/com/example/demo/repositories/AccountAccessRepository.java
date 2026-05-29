package com.example.demo.repositories;

import com.example.demo.domain.AccountAccess;
import com.example.demo.dto.AccountCurrencySummaryDTO;
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

    List<AccountAccess> findByUserUserIdAndStatus(int userId, String status);
    Optional<AccountAccess> findByAccountAccountIdAndUserUserIdAndStatus(Long accountId, int userId, String status);
    Optional<AccountAccess> findByAccountAccountIdAndUserEmail(Long accountId, String email);
    List<AccountAccess> findByAccountAccountId(Long accountId);
    @Query("""
        select aa
        from   AccountAccess aa
        join   aa.account a
        where  aa.user.userId = :userId
        and    aa.status = 'ACTIVE'
        and    a.status = 'ACTIVE'
    """)
    Page<AccountAccess> findActiveAccountsForUser(@Param("userId") int userId, Pageable pageable);
    @Query("""
        select new com.example.demo.dto.AccountCurrencySummaryDTO(a.currency, sum(a.balance), count(a))
        from   AccountAccess aa
        join aa.account a
        where  aa.user.userId = :userId
        and    aa.status = 'ACTIVE'
        and    a.status = 'ACTIVE'
        group by a.currency
    """)
    List<AccountCurrencySummaryDTO> getCurrencySummaryForUser(@Param("userId") int userId);
}
