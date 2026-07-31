package com.audit.transaction_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long primaryId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private String evaluationStrategy;

    @Column(nullable = false)
    private double executionTimeMs;

    private String transactionStatus;
    private Double riskScore;
    private LocalDateTime createdAt;
    private double v1;
    private double v2;
    private double v3;
    private double v4;
    private double v5;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Transaction() {}

    public Long getPrimaryId() { return primaryId; }
    public void setPrimaryId(Long primaryId) { this.primaryId = primaryId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getEvaluationStrategy() { return evaluationStrategy; }
    public void setEvaluationStrategy(String evaluationStrategy) { this.evaluationStrategy = evaluationStrategy; }

    public double getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(double executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getV1() { return v1; }
    public void setV1(double v1) { this.v1 = v1; }

    public double getV2() { return v2; }
    public void setV2(double v2) { this.v2 = v2; }

    public double getV3() { return v3; }
    public void setV3(double v3) { this.v3 = v3; }

    public double getV4() { return v4; }
    public void setV4(double v4) { this.v4 = v4; }

    public double getV5() { return v5; }
    public void setV5(double v5) { this.v5 = v5; }
}