package com.audit.transaction_service.dto;

import java.math.BigDecimal;

public class ResponseDto {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String transactionType;
    private double riskScore;
    private String transactionStatus;
    private long executionTimeMs;


    // Strategy toggle (e.g: IN_MEMORY_RULES | REMOTE_MOCK_AI | REMOTE_ACTIVE_AI )
    private String strategy;

    public ResponseDto() {}

    // Convenient parameterized constructor to quickly build the response in your Service layer
    public ResponseDto(String transactionId, double riskScore, String transactionStatus, String strategy, long executionTimeMs) {
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.transactionStatus= transactionStatus;
        this.strategy= strategy;
        this.executionTimeMs = executionTimeMs;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
