package com.example.transactionservice.repositories;

import com.example.transactionservice.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByStatusAndIsUrgentAndIsScheduled(String status, String isUrgent, String isScheduled);

    Page<Transaction> findByInitiatedByUserId(Integer initiatedByUserId, Pageable pageable);

    @Query("""
        select distinct t
        from   Transaction t
        where  t.initiatedByUserId = :userId
        or     t.sourceAccountId in :accountIds
        or     t.destinationAccountId in :accountIds
    """)
    Page<Transaction> findTransactionsForUserOrAccounts(
            @Param("userId") Integer userId,
            @Param("accountIds") Collection<Long> accountIds,
            Pageable pageable
    );

    @Query("""
        select t
        from   Transaction t
        where  t.sourceAccountId = :accountId
        or     t.destinationAccountId = :accountId
    """)
    Page<Transaction> findTransactionsForAccount(@Param("accountId") Long accountId, Pageable pageable);
}
