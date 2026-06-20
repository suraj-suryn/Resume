package com.suraj.banking.auth.repository;

import com.suraj.banking.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    // Custom JPQL — demonstrates Spring Data JPA query skills from resume
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.active = true ORDER BY a.createdAt DESC")
    List<Account> findActiveAccountsByUser(@Param("userId") Long userId);
}
