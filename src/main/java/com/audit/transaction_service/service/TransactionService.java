package com.audit.transaction_service.service;

import com.audit.transaction_service.model.Transaction;
import com.audit.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// @Slf4j creates logging
@Slf4j
// @Service makes Spring Boot treat this as a servic
@Service
public class TransactionService {

    // Bridge TransactionRepository interface to variable transactionRepository
    private final TransactionRepository transactionRepository;

    // Initialize risk evaluation constants
    private final static BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private final static double RISK_PENALTY_HIGH_VALUE = 0.5;
    private final static double RISK_THRESHOLD = 0.5;

    // Feature & Benchmarking Toggles
    @Value("${app.ai.risk-assessment.enabled:false}")
    private boolean aiRiskAssessmentEnabled;

    @Value("${app.ai.fallback.enabled:true}")
    private boolean aiFallbackEnabled;

    @Value("${app.ai.mock-risk-score:0.85}")
    private double mockAiRiskScore;

    @Value("${app.ai.service-url:}")
    private String aiServiceUrl;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction processTransaction(Transaction transaction) {
        // Standardized Guard Clause: Null payload, missing amount, or non-positive amount
        if (transaction == null || transaction.getTransactionAmount() == null ||
                transaction.getTransactionAmount().compareTo(BigDecimal.ZERO) <= 0) {

            String txIdStr = (transaction != null && transaction.getPrimaryId() != null)
                    ? String.valueOf(transaction.getPrimaryId())
                    : "UNKNOWN";

            log.error("[Transaction ID: {}] Rejecting processing: Payload is null or amount is <= 0.", txIdStr);
            throw new IllegalArgumentException(
                    String.format("[Transaction ID: %s] Transaction payload and amount must be present and greater than zero.", txIdStr)
            );
        }

        Long txId = transaction.getPrimaryId();
        double risk = 0;

        // Risk calculation strategy selection
        if (aiRiskAssessmentEnabled) {
            risk = calculateAiRiskScore(transaction);
        } else {
            risk = calculateLocalFallbackRiskScore(transaction);
        }

        // Apply calculated risk score
        transaction.setRiskScore(risk);

        // Evaluate risk against threshold
        if (transaction.getRiskScore() >= RISK_THRESHOLD) {
            transaction.setTransactionStatus("FLAGGED");
            log.warn("[Transaction ID: {}] Flagged with high risk score: {}", txId, risk);
        } else {
            transaction.setTransactionStatus("APPROVED");
            log.info("[Transaction ID: {}] Approved with risk score: {}", txId, risk);
        }

        // Save transaction entity
        return transactionRepository.save(transaction);
    }

    private double calculateAiRiskScore(Transaction transaction) {
        Long txId = transaction.getPrimaryId();

        if (aiServiceUrl == null || aiServiceUrl.isBlank()) {
            log.warn("[Transaction ID: {}] AI Service URL is missing or unconfigured.", txId);

            if (!aiFallbackEnabled) {
                log.error("[Transaction ID: {}] AI Service URL missing and fallback mode is disabled.", txId);
                throw new IllegalStateException(
                        String.format("[Transaction ID: %d] AI service URL is unconfigured and fallback mode is disabled.", txId)
                );
            }

            log.info("[Transaction ID: {}] Route shifted to local rule fallback.", txId);
            return calculateLocalFallbackRiskScore(transaction);
        }

        try {
            log.info("[Transaction ID: {}] Initiating AI API risk request to {}", txId, aiServiceUrl);
            return mockAiRiskScore;

        } catch (Exception e) {
            log.error("[Transaction ID: {}] Failed to reach AI service at {}. Reason: {}", txId, aiServiceUrl, e.getMessage());

            // Rethrow exception in Benchmarking Mode so JMeter/k6 captures HTTP 500
            if (!aiFallbackEnabled) {
                throw new RuntimeException(
                        String.format("[Transaction ID: %d] AI Service Failure during benchmarking mode: %s", txId, e.getMessage()), e
                );
            }

            // Resiliency Mode Fallback
            log.warn("[Transaction ID: {}] Fallback enabled. Defaulting to local rule calculation.", txId);
            return calculateLocalFallbackRiskScore(transaction);
        }
    }

    private double calculateLocalFallbackRiskScore(Transaction transaction) {
        double localRisk = 0.0;

        if (transaction.getTransactionAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            localRisk += RISK_PENALTY_HIGH_VALUE;
            log.info("[Transaction ID: {}] High-value threshold exceeded ( > {} ). Applied penalty score: +{}",
                    transaction.getPrimaryId(), HIGH_VALUE_THRESHOLD, RISK_PENALTY_HIGH_VALUE);
        }

        return localRisk;
    }
}