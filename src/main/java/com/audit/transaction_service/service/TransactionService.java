
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

// @Slf4j creates logging
@Slf4j
// @Service makes Spring Boot treat this as a servic
@Service
public class TransactionService {

    // Bridge TransactionRepository interface to variable transactionRepository
    private final TransactionRepository transactionRepository;
    private final WebClient webClient;

    // Initialize risk evaluation constants
    private final static BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private final static double RISK_PENALTY_HIGH_VALUE = 0.5;
    private final static double RISK_THRESHOLD = 0.5;

    @Value("${app.db.save.enabled:true}")
    private boolean dbSaveEnabled;

    @Value("${app.ai.mock-risk-score:0.85}")
    private double mockAiRiskScore;

    public TransactionService(TransactionRepository transactionRepository, WebClient webClient) {
        this.transactionRepository = transactionRepository;
        this.webClient = webClient;
    }

    @Transactional
    public ResponseDto processTransaction(RequestDto request) {
        long startTime = System.currentTimeMillis();

        // Guard Clause
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            String txIdStr = (request != null && request.getTransactionId() != null) ? request.getTransactionId() : "UNKNOWN";
            log.error("[Transaction ID: {}] Rejecting processing: Payload is null or amount <= 0.", txIdStr);
            throw new IllegalArgumentException(
                    String.format("[Transaction ID: %s] Transaction payload and amount must be present and > 0.", txIdStr)
            );
        }

        String strategy = (request.getStrategy() != null) ? request.getStrategy().toUpperCase() : "IN_MEMORY_RULES";
        double riskScore = 0.0;

        // routing strategies switch case statement

        switch (strategy) {
            case "REMOTE_ACTIVE_AI":
                riskScore = fetchRemoteAiRiskScore(request, "/predict");
                break;
            case "REMOTE_MOCK_AI":
                riskScore = fetchRemoteAiRiskScore(request, "/predict/mock");
                break;
            case "IN_MEMORY_RULES":
            default:
                riskScore = calculateLocalRuleRiskScore(request);
                log.info("[Transaction ID: {}] Evaluated via IN_MEMORY_RULES: {}", request.getTransactionId(), riskScore);
                break;
                //defaults anything else to in memory rules
        }

        // Risk Status Evaluation
        String status = (riskScore >= RISK_THRESHOLD) ? "FLAGGED" : "APPROVED";

        // Record execution time before DB writes
        long executionTimeMs = System.currentTimeMillis() - startTime;

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
            entity.setExecutionTimeMs(executionTimeMs);

            transactionRepository.save(entity);
            log.debug("[Transaction ID: {}] Saved to H2 database.", request.getTransactionId());
        } else {
            log.debug("[Transaction ID: {}] DB persistence bypassed via config flag.", request.getTransactionId());
        }

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

        return response;
    }

    private double fetchRemoteAiRiskScore(RequestDto request, String endpoint) {
        log.info("[Transaction ID: {}] Sending HTTP POST to Python endpoint {}", request.getTransactionId(), endpoint);

        try {
            // Map the 6 target features ($V_1 \dots V_5$ + amount) to the AI DTO
            AiRequestDto aiPayload = new AiRequestDto(
                    request.getAmount().doubleValue(),
                    request.getV1(),
                    request.getV2(),
                    request.getV3(),
                    request.getV4(),
                    request.getV5()
            );

            // Execute non-blocking call synchronously via .block() for single-request telemetry
            AiRiskResponse response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(aiPayload)
                    .retrieve()
                    .bodyToMono(AiRiskResponse.class)
                    .block();

            return (response != null) ? response.getRiskScore() : 0.0;

        } catch (Exception e) {
            log.error("[Transaction ID: {}] Failed to communicate with Python endpoint {}: {}",
                    request.getTransactionId(), endpoint, e.getMessage());
            throw new RuntimeException("AI service communication error: " + e.getMessage(), e);
        }
    }

    private double calculateLocalRuleRiskScore(RequestDto request) {
        double localRisk = 0.0;

        // Convert BigDecimal amount to double once for floating-point math
        double amount = (request.getAmount() != null) ? request.getAmount().doubleValue() : 0.0;

        // High value check
        if (amount > HIGH_VALUE_THRESHOLD.doubleValue()) {
            localRisk += RISK_PENALTY_HIGH_VALUE;

            // Sub tree branch A: Evaluate V1 & V2 interaction
            if (request.getV1() > 2.5 || request.getV2() < -1.0) {
                localRisk += 0.2;
            }
        } else {
            // Sub tree branch B: Low value, check V3 anomaly
            if (request.getV3() > 3.0) {
                localRisk += 0.15;
            }
        }

        // Node 2: Multi-feature combined heuristic (simulates tree depth)
        if (request.getV4() > 1.5 && request.getV5() < -0.5) {
            localRisk += 0.15;
        }

        // Normalize risk score between 0.0 and 1.0 (matching probability bounds)
        return Math.min(1.0, localRisk);
    }

    private static class AiRiskResponse {
        private double riskScore;

        public double getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(double riskScore) {
            this.riskScore = riskScore;
        }
    }


}
