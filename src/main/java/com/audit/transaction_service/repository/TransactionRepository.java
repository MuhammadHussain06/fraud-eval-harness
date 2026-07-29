package com.audit.transaction_service.repository;

import com.audit.transaction_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// @Repository makes springboot treat this as a repository interface
@Repository
// TransactionRepository interface initialization using JpaRepository targeting Transaction @entity
// which has a primary key with data value type long
public interface TransactionRepository extends JpaRepository <Transaction, Long> {

    // interface method to be able to return a list of transactions with related data from Transaction @entity
    // using account id to search for it
   List<Transaction> findByAccountId(String accountID);

    // Fetch transactions by evaluation strategy
   List<Transaction> findByEvaluationStrategy(String evaluationStrategy);

    // Fetch a single transaction by its unique transaction ID
   Optional<Transaction> findByTransactionId(String transactionId);

}
