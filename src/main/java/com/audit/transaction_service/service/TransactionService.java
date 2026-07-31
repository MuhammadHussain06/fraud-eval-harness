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

    private final static BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private final static double RISK_PENALTY_HIGH_VALUE = 0.5;
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
        log.warn(" TESTBED INITIALIZATION: Database automatically cleared for clean benchmark state.");
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
        long requestParsingTimeMs = (System.nanoTime() - parseStart) / 1_000_000;

        String strategy = (request.getStrategy() != null) ? request.getStrategy().toUpperCase() : "IN_MEMORY_RULES";
        double riskScore = 0.0;
        long networkCommunicationTimeMs = 0;
        long evaluationLogicTimeMs = 0;
        ResponseDto.PythonTelemetryDto pythonTelemetry = new ResponseDto.PythonTelemetryDto();

        // 2. Evaluation & Strategy Execution Phase
        long evalStart = System.nanoTime();
        switch (strategy) {
            case "REMOTE_ACTIVE_AI":
                AiTimingResult activeResult = fetchRemoteAiRiskScore(request, "/predict");
                riskScore = activeResult.riskScore;
                networkCommunicationTimeMs = activeResult.networkTimeMs;
                if (activeResult.pythonTelemetry != null) {
                    pythonTelemetry = activeResult.pythonTelemetry;
                }
                evaluationLogicTimeMs = Math.max(0, ((System.nanoTime() - evalStart) / 1_000_000) - networkCommunicationTimeMs);
                break;
            case "REMOTE_MOCK_AI":
                AiTimingResult mockResult = fetchRemoteAiRiskScore(request, "/predict/mock");
                riskScore = mockResult.riskScore;
                networkCommunicationTimeMs = mockResult.networkTimeMs;
                if (mockResult.pythonTelemetry != null) {
                    pythonTelemetry = mockResult.pythonTelemetry;
                }
                evaluationLogicTimeMs = Math.max(0, ((System.nanoTime() - evalStart) / 1_000_000) - networkCommunicationTimeMs);
                break;
            case "IN_MEMORY_RULES":
            default:
                long ruleStart = System.nanoTime();
                riskScore = calculateLocalRuleRiskScore(request);
                evaluationLogicTimeMs = (System.nanoTime() - ruleStart) / 1_000_000;
                log.info("[Transaction ID: {}] Evaluated via IN_MEMORY_RULES in {} ms: {}", request.getTransactionId(), evaluationLogicTimeMs, riskScore);
                break;
        }

        // Risk Status Evaluation
        String status = (riskScore >= RISK_THRESHOLD) ? "FLAGGED" : "APPROVED";

        // 3. Database Write Persistence Phase
        long dbStart = System.nanoTime();
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
            entity.setExecutionTimeMs((System.nanoTime() - overallStartTime) / 1_000_000);

            transactionRepository.save(entity);
            log.debug("[Transaction ID: {}] Saved to H2 database.", request.getTransactionId());
        } else {
            log.debug("[Transaction ID: {}] DB persistence bypassed via config flag.", request.getTransactionId());
        }
        long dbWriteTimeMs = (System.nanoTime() - dbStart) / 1_000_000;

        // 4. Response Building & Serialization Phase
        long responseBuildStart = System.nanoTime();
        long executionTimeMs = (System.nanoTime() - overallStartTime) / 1_000_000;

        log.info("[Transaction ID: {}] Executed strategy [{}] in {} ms | Status: {}",
                request.getTransactionId(), strategy, executionTimeMs, status);

        // Build and return ResponseDto
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

        // Populate metrics
        response.setRequestParsingTimeMs(requestParsingTimeMs);
        response.setEvaluationLogicTimeMs(evaluationLogicTimeMs);
        response.setNetworkCommunicationTimeMs(networkCommunicationTimeMs);
        response.setDbWriteTimeMs(dbWriteTimeMs);
        response.setPythonTelemetry(pythonTelemetry);

        long responseSerializationTimeMs = (System.nanoTime() - responseBuildStart) / 1_000_000;
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

            long netDurationMs = (System.nanoTime() - netStart) / 1_000_000;
            double score = (response != null) ? response.getRiskScore() : 0.0;
            ResponseDto.PythonTelemetryDto telemetry = (response != null && response.getPythonTelemetry() != null)
                    ? response.getPythonTelemetry()
                    : new ResponseDto.PythonTelemetryDto();

            return new AiTimingResult(score, netDurationMs, telemetry);

        } catch (Exception e) {
            long netDurationMs = (System.nanoTime() - netStart) / 1_000_000;
            log.error("[Transaction ID: {}] Failed to communicate with Python endpoint {}: {}",
                    request.getTransactionId(), endpoint, e.getMessage());
            return new AiTimingResult(0.0, netDurationMs, new ResponseDto.PythonTelemetryDto());
        }
    }

    private double calculateLocalRuleRiskScore(RequestDto request) {
        double localRisk = 0.0;
        double amount = (request.getAmount() != null) ? request.getAmount().doubleValue() : 0.0;

        if (amount > HIGH_VALUE_THRESHOLD.doubleValue()) {
            localRisk += RISK_PENALTY_HIGH_VALUE;
            if (request.getV1() > 2.5 || request.getV2() < -1.0) {
                localRisk += 0.2;
            }
        } else {
            if (request.getV3() > 3.0) {
                localRisk += 0.15;
            }
        }

        if (request.getV4() > 1.5 && request.getV5() < -0.5) {
            localRisk += 0.15;
        }

        return Math.min(1.0, localRisk);
    }

    private static class AiTimingResult {
        double riskScore;
        long networkTimeMs;
        ResponseDto.PythonTelemetryDto pythonTelemetry;

        public AiTimingResult(double riskScore, long networkTimeMs, ResponseDto.PythonTelemetryDto pythonTelemetry) {
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