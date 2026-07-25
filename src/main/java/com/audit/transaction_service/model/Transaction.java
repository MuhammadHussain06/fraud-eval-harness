package com.audit.transaction_service.model;
// import for database annotations
import jakarta.persistence.*;
// import for currency number format
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @Entity makes springboot treat this class as a database table
@Entity
// names the database table
@Table(name = "transactions")


public class Transaction {
    // sets primary key through JPA
    @Id
    // sets automated strategy for id generation
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // initialize id
    private Long primaryId;

    // creates column through JPA which rejects null saves
    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    // BigDecimal datatype immutable for currency to avoid rounding error
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private String transactionType;

    private String transactionStatus;

    private Double riskScore;

    private LocalDateTime createdAt;

    // JPA lifecycle notation to run the given method before saving a new row to the database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // required empty constructor by JPA
    public Transaction() {
    }

    // Parameterized constructor to quickly create new transactions
    public Transaction(String accountId, BigDecimal amount, String transactionType) {
        this.accountId = accountId;
        this.transactionAmount = amount;
        this.transactionType = transactionType;
        this.transactionStatus = "PENDING"; // Default status when created
    }

    public Long getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(Long primaryId) {
        this.primaryId = primaryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
