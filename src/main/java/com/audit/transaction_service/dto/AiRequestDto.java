package com.audit.transaction_service.dto;

public class AiRequestDto {

    private double transactionAmount;
    private double v1;
    private double v2;
    private double v3;
    private double v4;
    private double v5;

    public AiRequestDto() {}

    public AiRequestDto(double transactionAmount, double v1, double v2, double v3, double v4, double v5) {
        this.transactionAmount = transactionAmount;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
    }

    public double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
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