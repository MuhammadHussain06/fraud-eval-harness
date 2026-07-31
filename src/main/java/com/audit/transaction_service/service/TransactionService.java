package com.audit.transaction_service.service;

import com.audit.transaction_service.dto.AiRequestDto;
import com.audit.transaction_service.dto.RequestDto;
import com.audit.transaction_service.dto.ResponseDto;
import com.audit.transaction_service.model.Transaction;
import com.audit.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WebClient webClient;

    private final static double RISK_THRESHOLD = 0.5;

    @Value("${app.db.save.enabled:true}")
    private boolean dbSaveEnabled;

    public TransactionService(TransactionRepository transactionRepository, WebClient webClient) {
        this.transactionRepository = transactionRepository;
        this.webClient = webClient;
    }

    @jakarta.annotation.PostConstruct
    public void clearDatabaseOnStartup() {
        transactionRepository.deleteAll();
        log.warn("TESTBED INITIALIZATION: Database automatically cleared for clean benchmark state.");
    }

    @Transactional
    public ResponseDto processTransaction(RequestDto request) {
        long overallStartTime = System.nanoTime();

        // 1. Request Parsing & Guard Clause Validation Phase
        long parseStart = System.nanoTime();
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            String txIdStr = (request != null && request.getTransactionId() != null) ? request.getTransactionId() : "UNKNOWN";
            log.error("[Transaction ID: {}] Rejecting processing: Payload is null or amount <= 0.", txIdStr);
            throw new IllegalArgumentException(
                    String.format("[Transaction ID: %s] Transaction payload and amount must be present and > 0.", txIdStr)
            );
        }
        double requestParsingTimeMs = (System.nanoTime() - parseStart) / 1_000_000.0;

        String strategy = request.getStrategy().toUpperCase();
        double riskScore = 0.0;
        double networkCommunicationTimeMs = 0.0;
        ResponseDto.PythonTelemetryDto pythonTelemetry = new ResponseDto.PythonTelemetryDto();

        //  Distributed AI Strategy Execution & Remote Delegation Phase
        switch (strategy) {
            case "DISTRIBUTED_AI_SYNCHRONOUS":
                AiTimingResult activeResult = fetchRemoteAiRiskScore(request, "/predict");
                riskScore = activeResult.riskScore;
                networkCommunicationTimeMs = activeResult.networkTimeMs;
                if (activeResult.pythonTelemetry != null) {
                    pythonTelemetry = activeResult.pythonTelemetry;
                }
                break;

            case "DISTRIBUTED_MOCK_GATEWAY":
                AiTimingResult mockResult = fetchRemoteAiRiskScore(request, "/predict/mock");
                riskScore = mockResult.riskScore;
                networkCommunicationTimeMs = mockResult.networkTimeMs;
                if (mockResult.pythonTelemetry != null) {
                    pythonTelemetry = mockResult.pythonTelemetry;
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid evaluation strategy topology provided: " + strategy);
        }

        // Risk Status
        String status = (riskScore >= RISK_THRESHOLD) ? "FLAGGED" : "APPROVED";

        // Database Write Persistence
        long dbStart = System.nanoTime();
        double currentExecutionTimeMs = (System.nanoTime() - overallStartTime) / 1_000_000.0;
        if (dbSaveEnabled) {
            Transaction entity = new Transaction();
            entity.setTransactionId(request.getTransactionId());
            entity.setAccountId(request.getAccountId());
            entity.setTransactionAmount(request.getAmount());
            entity.setTransactionType(request.getTransactionType());
            entity.setV1(request.getV1());
            entity.setV2(request.getV2());
            entity.setV3(request.getV3());
            entity.setV4(request.getV4());
            entity.setV5(request.getV5());
            entity.setRiskScore(riskScore);
            entity.setTransactionStatus(status);
            entity.setEvaluationStrategy(strategy);
            entity.setExecutionTimeMs(currentExecutionTimeMs);

            transactionRepository.save(entity);
            log.debug("[Transaction ID: {}] Saved to H2 database.", request.getTransactionId());
        } else {
            log.debug("[Transaction ID: {}] DB persistence bypassed via configuration flag.", request.getTransactionId());
        }
        double dbWriteTimeMs = (System.nanoTime() - dbStart) / 1_000_000.0;

        //  Response Building & Serialization
        long responseBuildStart = System.nanoTime();
        double executionTimeMs = (System.nanoTime() - overallStartTime) / 1_000_000.0;

        log.info("[Transaction ID: {}] Executed strategy [{}] in {} ms | Status: {}",
                request.getTransactionId(), strategy, executionTimeMs, status);

        ResponseDto response = new ResponseDto(
                request.getTransactionId(),
                riskScore,
                status,
                strategy,
                executionTimeMs
        );
        response.setAccountId(request.getAccountId());
        response.setAmount(request.getAmount());
        response.setTransactionType(request.getTransactionType());

        // Populate telemetry metrics
        response.setRequestParsingTimeMs(requestParsingTimeMs);
        response.setNetworkCommunicationTimeMs(networkCommunicationTimeMs);
        response.setDbWriteTimeMs(dbWriteTimeMs);
        response.setPythonTelemetry(pythonTelemetry);

        double responseSerializationTimeMs = (System.nanoTime() - responseBuildStart) / 1_000_000.0;
        response.setResponseSerializationTimeMs(responseSerializationTimeMs);

        return response;
    }

    private AiTimingResult fetchRemoteAiRiskScore(RequestDto request, String endpoint) {
        log.info("[Transaction ID: {}] Sending HTTP POST to Python endpoint {}", request.getTransactionId(), endpoint);
        long netStart = System.nanoTime();

        try {
            AiRequestDto aiPayload = new AiRequestDto(
                    request.getAmount().doubleValue(),
                    request.getV1(),
                    request.getV2(),
                    request.getV3(),
                    request.getV4(),
                    request.getV5()
            );

            AiRiskResponse response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(aiPayload)
                    .retrieve()
                    .bodyToMono(AiRiskResponse.class)
                    .block();

            double netDurationMs = (System.nanoTime() - netStart) / 1_000_000.0;
            double score = (response != null) ? response.getRiskScore() : 0.0;
            ResponseDto.PythonTelemetryDto telemetry = (response != null && response.getPythonTelemetry() != null)
                    ? response.getPythonTelemetry()
                    : new ResponseDto.PythonTelemetryDto();

            return new AiTimingResult(score, netDurationMs, telemetry);

        } catch (Exception e) {
            double netDurationMs = (System.nanoTime() - netStart) / 1_000_000.0;
            log.error("[Transaction ID: {}] Failed to communicate with Python endpoint {}: {}",
                    request.getTransactionId(), endpoint, e.getMessage());
            return new AiTimingResult(0.0, netDurationMs, new ResponseDto.PythonTelemetryDto());
        }
    }

    private static class AiTimingResult {
        double riskScore;
        double networkTimeMs;
        ResponseDto.PythonTelemetryDto pythonTelemetry;

        public AiTimingResult(double riskScore, double networkTimeMs, ResponseDto.PythonTelemetryDto pythonTelemetry) {
            this.riskScore = riskScore;
            this.networkTimeMs = networkTimeMs;
            this.pythonTelemetry = pythonTelemetry;
        }
    }

    private static class AiRiskResponse {
        private double riskScore;
        private ResponseDto.PythonTelemetryDto pythonTelemetry;

        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

        public ResponseDto.PythonTelemetryDto getPythonTelemetry() { return pythonTelemetry; }
        public void setPythonTelemetry(ResponseDto.PythonTelemetryDto pythonTelemetry) { this.pythonTelemetry = pythonTelemetry; }
    }
}