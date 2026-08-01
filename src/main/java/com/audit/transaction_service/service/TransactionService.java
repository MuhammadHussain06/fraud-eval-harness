package com.audit.transaction_service.service;

import com.audit.transaction_service.dto.AiRequestDto;
import com.audit.transaction_service.dto.RequestDto;
import com.audit.transaction_service.dto.ResponseDto;
import com.audit.transaction_service.model.Transaction;
import com.audit.transaction_service.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WebClient webClient;

    private final static double RISK_THRESHOLD = 0.5;

    @Value("${app.db.save.enabled:true}")
    private boolean dbSaveEnabled;

    // Constructor injection for repository and reactive web client
    public TransactionService(TransactionRepository transactionRepository, WebClient webClient) {
        this.transactionRepository = transactionRepository;
        this.webClient = webClient;
    }


    //  return type is now Mono<ResponseDto>
    public Mono<ResponseDto> processTransaction(RequestDto request) {
        long overallStartTime = System.nanoTime();

        // Request Parsing & Guard Clause Validation Phase
        long parseStart = System.nanoTime();
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            String txIdStr = (request != null && request.getTransactionId() != null) ? request.getTransactionId() : "UNKNOWN";
            log.error("[Transaction ID: {}] Rejecting processing: Payload is null or amount <= 0.", txIdStr);
            // Returns a reactive error signal down the chain
            return Mono.error(new IllegalArgumentException(
                    String.format("[Transaction ID: %s] Transaction payload and amount must be present and > 0.", txIdStr)
            ));
        }
        double requestParsingTimeMs = (System.nanoTime() - parseStart) / 1_000_000.0;

        String strategy = request.getStrategy().toUpperCase();
        String endpoint;

        //Multi-Strategy Endpoint Resolution
        switch (strategy) {
            case "DISTRIBUTED_AI_SYNCHRONOUS":
                endpoint = "/predict";
                break;
            case "DISTRIBUTED_MOCK_GATEWAY":
                endpoint = "/predict/mock";
                break;
            default:
                return Mono.error(new IllegalArgumentException("Invalid evaluation strategy topology provided: " + strategy));
        }

        log.info("[Transaction ID: {}] Sending HTTP POST to Python endpoint {}", request.getTransactionId(), endpoint);
        long netStart = System.nanoTime();

        // Build the payload transport object destined for Python
        AiRequestDto aiPayload = new AiRequestDto(
                request.getAmount().doubleValue(),
                request.getV1(),
                request.getV2(),
                request.getV3(),
                request.getV4(),
                request.getV5()
        );

        // Non-blocking Execution Chain via WebClient & Project Reactor
        return webClient.post()
                .uri(endpoint)
                .bodyValue(aiPayload)
                .retrieve()
                .bodyToMono(AiRiskResponse.class) // Asynchronously parses JSON response into a Mono
                .map(aiResponse -> {
                    //  executes non-blocking style once the AI response arrives
                    double networkCommunicationTimeMs = (System.nanoTime() - netStart) / 1_000_000.0;
                    double riskScore = (aiResponse != null) ? aiResponse.getRiskScore() : 0.0;
                    ResponseDto.PythonTelemetryDto pythonTelemetry = (aiResponse != null && aiResponse.getPythonTelemetry() != null)
                            ? aiResponse.getPythonTelemetry()
                            : new ResponseDto.PythonTelemetryDto();

                    return new IntermediateResult(riskScore, networkCommunicationTimeMs, pythonTelemetry);
                })
                .onErrorResume(e -> {
                    // Fallback operator
                    double networkCommunicationTimeMs = (System.nanoTime() - netStart) / 1_000_000.0;
                    log.error("[Transaction ID: {}] Failed to communicate with Python endpoint {}: {}",
                            request.getTransactionId(), endpoint, e.getMessage());
                    return Mono.just(new IntermediateResult(0.0, networkCommunicationTimeMs, new ResponseDto.PythonTelemetryDto()));
                })
                .flatMap(intermediate -> {
                    // Unpacks the intermediate container to finish database persistence and response building
                    double riskScore = intermediate.riskScore;
                    double networkCommunicationTimeMs = intermediate.networkTimeMs;
                    ResponseDto.PythonTelemetryDto pythonTelemetry = intermediate.pythonTelemetry;

                    // Evaluate risk status
                    String status = (riskScore >= RISK_THRESHOLD) ? "FLAGGED" : "APPROVED";

                    // Database Write Persistence
                    long dbStart = System.nanoTime();
                    double currentExecutionTimeMs = (System.nanoTime() - overallStartTime) / 1_000_000.0;

                    // Wrapped the database save logic into a reactive container (Mono) so the pipeline waits for it to finish instead of dropping it.
                    Mono<Void> dbMono;
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

                        dbMono = transactionRepository.save(entity)
                                .doOnSuccess(saved -> log.debug("[Transaction ID: {}] Saved to H2 database.", request.getTransactionId()))
                                .then();
                    } else {
                        log.debug("[Transaction ID: {}] DB persistence bypassed via configuration flag.", request.getTransactionId());
                        dbMono = Mono.empty();
                    }

                    return dbMono.map(unused -> {
                        double dbWriteTimeMs = (System.nanoTime() - dbStart) / 1_000_000.0;

                        // Response Building & Serialization
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

                        // Populate complete telemetry metrics tracking
                        response.setRequestParsingTimeMs(requestParsingTimeMs);
                        response.setNetworkCommunicationTimeMs(networkCommunicationTimeMs);
                        response.setDbWriteTimeMs(dbWriteTimeMs);
                        response.setPythonTelemetry(pythonTelemetry);

                        double responseSerializationTimeMs = (System.nanoTime() - responseBuildStart) / 1_000_000.0;
                        response.setResponseSerializationTimeMs(responseSerializationTimeMs);

                        // Wraps the final populated DTO back into a reactive Mono container
                        return response;
                    });
                });
    }

    // Helper holder class to pass intermediate network values down the reactive chain
    private static class IntermediateResult {
        double riskScore;
        double networkTimeMs;
        ResponseDto.PythonTelemetryDto pythonTelemetry;

        public IntermediateResult(double riskScore, double networkTimeMs, ResponseDto.PythonTelemetryDto pythonTelemetry) {
            this.riskScore = riskScore;
            this.networkTimeMs = networkTimeMs;
            this.pythonTelemetry = pythonTelemetry;
        }
    }

    // Internal mapping container for incoming Python JSON payloads
    private static class AiRiskResponse {
        private double riskScore;
        private ResponseDto.PythonTelemetryDto pythonTelemetry;

        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

        public ResponseDto.PythonTelemetryDto getPythonTelemetry() { return pythonTelemetry; }
        public void setPythonTelemetry(ResponseDto.PythonTelemetryDto pythonTelemetry) { this.pythonTelemetry = pythonTelemetry; }
    }
}