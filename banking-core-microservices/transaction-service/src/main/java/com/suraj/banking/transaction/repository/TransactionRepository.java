package com.suraj.banking.transaction.repository;

import com.suraj.banking.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            String fromAccountId, String toAccountId);
}
