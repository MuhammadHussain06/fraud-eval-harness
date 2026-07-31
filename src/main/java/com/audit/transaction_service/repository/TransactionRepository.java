package com.audit.transaction_service.repository;

import com.audit.transaction_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(String accountId);
    List<Transaction> findByEvaluationStrategy(String evaluationStrategy);
    Optional<Transaction> findByTransactionId(String transactionId);
}