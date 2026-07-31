package com.audit.transaction_service.dto;

import java.math.BigDecimal;

public class ResponseDto {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String transactionType;
    private double riskScore;
    private String transactionStatus;

    // Java-side telemetry timing fields
    private long executionTimeMs;
    private long requestParsingTimeMs;
    private long evaluationLogicTimeMs;
    private long networkCommunicationTimeMs;
    private long dbWriteTimeMs;
    private long responseSerializationTimeMs;

    // Python-side telemetry timing metrics container
    private PythonTelemetryDto pythonTelemetry = new PythonTelemetryDto();

    // Strategy toggle (e.g: IN_MEMORY_RULES | REMOTE_MOCK_AI | REMOTE_ACTIVE_AI )
    private String strategy;

    public ResponseDto() {}

    public ResponseDto(String transactionId, double riskScore, String transactionStatus, String strategy, long executionTimeMs) {
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.transactionStatus = transactionStatus;
        this.strategy = strategy;
        this.executionTimeMs = executionTimeMs;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public long getRequestParsingTimeMs() { return requestParsingTimeMs; }
    public void setRequestParsingTimeMs(long requestParsingTimeMs) { this.requestParsingTimeMs = requestParsingTimeMs; }

    public long getEvaluationLogicTimeMs() { return evaluationLogicTimeMs; }
    public void setEvaluationLogicTimeMs(long evaluationLogicTimeMs) { this.evaluationLogicTimeMs = evaluationLogicTimeMs; }

    public long getNetworkCommunicationTimeMs() { return networkCommunicationTimeMs; }
    public void setNetworkCommunicationTimeMs(long networkCommunicationTimeMs) { this.networkCommunicationTimeMs = networkCommunicationTimeMs; }

    public long getDbWriteTimeMs() { return dbWriteTimeMs; }
    public void setDbWriteTimeMs(long dbWriteTimeMs) { this.dbWriteTimeMs = dbWriteTimeMs; }

    public long getResponseSerializationTimeMs() { return responseSerializationTimeMs; }
    public void setResponseSerializationTimeMs(long responseSerializationTimeMs) { this.responseSerializationTimeMs = responseSerializationTimeMs; }

    public PythonTelemetryDto getPythonTelemetry() { return pythonTelemetry; }
    public void setPythonTelemetry(PythonTelemetryDto pythonTelemetry) { this.pythonTelemetry = pythonTelemetry; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    // Nested DTO mapping Python's internal microservice performance stats
    public static class PythonTelemetryDto {
        private double parsingRequestTimeMs = 0.0;
        private double computationTimeMs = 0.0; // Captures ML inference or mock calculation time
        private double serializationResponseTimeMs = 0.0;
        private double totalPythonExecutionTimeMs = 0.0;

        public PythonTelemetryDto() {}

        public double getParsingRequestTimeMs() { return parsingRequestTimeMs; }
        public void setParsingRequestTimeMs(double parsingRequestTimeMs) { this.parsingRequestTimeMs = parsingRequestTimeMs; }

        public double getComputationTimeMs() { return computationTimeMs; }
        public void setComputationTimeMs(double computationTimeMs) { this.computationTimeMs = computationTimeMs; }

        public double getSerializationResponseTimeMs() { return serializationResponseTimeMs; }
        public void setSerializationResponseTimeMs(double serializationResponseTimeMs) { this.serializationResponseTimeMs = serializationResponseTimeMs; }

        public double getTotalPythonExecutionTimeMs() { return totalPythonExecutionTimeMs; }
        public void setTotalPythonExecutionTimeMs(double totalPythonExecutionTimeMs) { this.totalPythonExecutionTimeMs = totalPythonExecutionTimeMs; }
    }
}