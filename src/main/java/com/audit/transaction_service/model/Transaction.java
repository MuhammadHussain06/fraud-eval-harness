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

    @Column(nullable = false)
    private String transactionId;

    // creates column through JPA which rejects null saves
    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    // BigDecimal datatype immutable for currency to avoid rounding error
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private String evaluationStrategy;

    @Column(nullable = false)
    private long executionTimeMs;

    private String transactionStatus;

    private Double riskScore;

    private LocalDateTime createdAt;

    private double v1;

    private double v2;

    private double v3;

    private double v4;

    private double v5;

    // JPA lifecycle notation to run the given method before saving a new row to the database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // required empty constructor by JPA
    public Transaction() {
    }

    // Parameterized constructor to quickly create new transactions
    public Transaction(String accountId, BigDecimal amount, String transactionType, String transactionId, double v1, double v2, double v3, double v4, double v5, double riskScore, long executionTimeMs, String evaluationStrategy, String transactionStatus) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionAmount = amount;
        this.transactionType = transactionType;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
        this.riskScore = riskScore;
        this.transactionStatus = transactionStatus;
        this.evaluationStrategy = evaluationStrategy;
        this.executionTimeMs = executionTimeMs;
    }

    public Long getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(Long primaryId) {
        this.primaryId = primaryId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public String getEvaluationStrategy() {
        return evaluationStrategy;
    }

    public void setEvaluationStrategy(String evaluationStrategy) {
        this.evaluationStrategy = evaluationStrategy;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
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

    public double getV1() {
        return v1;
    }

    public void setV1(double v1) {
        this.v1 = v1;
    }

    public double getV2() {
        return v2;
    }

    public void setV2(double v2) {
        this.v2 = v2;
    }

    public double getV3() {
        return v3;
    }

    public void setV3(double v3) {
        this.v3 = v3;
    }

    public double getV4() {
        return v4;
    }

    public void setV4(double v4) {
        this.v4 = v4;
    }

    public double getV5() {
        return v5;
    }

    public void setV5(double v5) {
        this.v5 = v5;
    }
}
