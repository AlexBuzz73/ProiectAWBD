package com.example.demo.repositories;

import com.example.demo.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByStatusAndIsUrgentAndIsScheduled(String status, String isUrgent, String isScheduled);
    @Query("""
        select distinct t
        from   Transaction t
        where  exists (
            select 1
            from   AccountAccess aa
            join   aa.account a
            where  aa.user.userId = :userId
            and    aa.status = 'ACTIVE'
            and    a.status = 'ACTIVE'
            and    (
                       a = t.sourceAccount
                       or a = t.destinationAccount
            )
        )
    """)
    Page<Transaction> findTransactionsForUserAccounts(@Param("userId") int userId, Pageable pageable);
    @Query("""
        select t
        from   Transaction t
        where  t.sourceAccount.accountId = :accountId
        or     t.destinationAccount.accountId = :accountId
    """)
    Page<Transaction> findTransactionsForAccount(@Param("accountId") Long accountId, Pageable pageable);
}
