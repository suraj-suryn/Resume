package com.suraj.banking.auth.repository;

import com.suraj.banking.auth.entity.Transaction;
import com.suraj.banking.auth.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Paginated history — demonstrates pagination skills
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    // Named param query — filter by type
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.transactionType = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndType(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            Pageable pageable);

    // Date range filter — demonstrates complex JPA query from resume
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.createdAt BETWEEN :from AND :to ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
