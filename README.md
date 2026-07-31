# Fraud Evaluation Harness (Spring Boot + Python AI)

A high-throughput Java Spring Boot orchestrator built to serve as a benchmarking harness for real-time transaction fraud evaluation. 

This backend acts as the bridge between incoming client transactions and a downstream **Python FastAPI Risk Management Microservice** trained on the popular `creditcard.csv` dataset.

---

## Architectural Context & Purpose

This service acts as the central router and metrics engine designed to:

1. **Ingest & Validate**: Receive incoming transaction requests and validate critical features.
2. **Interface with AI/ML Services**: Communicate via non-blocking `WebClient` with a Python FastAPI microservice that hosts ML models trained on `creditcard.csv`.
3. **Feature Extraction**: Pass the core feature subset required by the trained model:
   * **`amount`**: The transaction value.
   * **`v1` – `v5`**: The first five PCA-transformed feature components extracted from the dataset.
4. **Benchmark & Persist**: Evaluate distributed system performance across experimental strategies (e.g., live synchronous AI inference via `DISTRIBUTED_AI_SYNCHRONOUS` vs. network-baseline mock routing via `DISTRIBUTED_MOCK_GATEWAY`), recording precise multi-tiered latencies (`executionTimeMs`, network communication time, DB write time) and decision outcomes to an embedded H2 database.

---

## Tech Stack

* **Java**: 21 (LTS)
* **Framework**: Spring Boot 3.2.5
* **Asynchronous I/O**: Spring WebFlux (`WebClient` for Python service communication)
* **Data & Persistence**: Spring Data JPA, H2 In-Memory Database
* **Validation & Utilities**: Lombok, Jakarta Bean Validation
* **Dataset Target**: `creditcard.csv` (Anonymized PCA features `v1`–`v5` + `amount`)

---

## API Endpoint & Payload Details

### `POST /api/v1/transactions`

Evaluates a transaction payload and returns the risk score, decision status (`APPROVED` / `FLAGGED`), strategy used, and processing latency.

#### **Sample Request Body**
```json
{
  "transactionId": "062e5e0e-398d-4e59-a29b-63175c8e345e",
  "accountId": "ACC-12345",
  "amount": 12500.50,
  "transactionType": "WIRE_TRANSFER",
  "v1": 2.8,
  "v2": -1.2,
  "v3": 0.5,
  "v4": 1.8,
  "v5": -0.8,
  "strategy": "DISTRIBUTED_AI_SYNCHRONOUS"
}