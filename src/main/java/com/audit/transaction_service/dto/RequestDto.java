package com.audit.transaction_service.dto;

import java.math.BigDecimal;

public class RequestDto {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String transactionType;

    private double v1;
    private double v2;
    private double v3;
    private double v4;
    private double v5;

    public RequestDto () {}

    // Strategy toggle (e.g: IN_MEMORY_RULES | REMOTE_MOCK_AI | REMOTE_ACTIVE_AI )
    private String strategy;

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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

}
