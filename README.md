# Fraud Evaluation Harness (Spring Boot + Python AI)

A high-throughput, non-blocking Java Spring Boot orchestrator built to serve as a benchmarking harness for real-time transaction fraud evaluation.

This backend acts as the bridge between incoming client transactions and a downstream **Python FastAPI Risk Management Microservice** (supporting both live AI inference and high-performance mock gateway controls).

---

## Architectural Context & Purpose

This service acts as the central router and metrics engine designed to:

1. **Ingest & Validate**: Receive incoming transaction requests and validate critical features using Jakarta Bean Validation.
2. **Interface with AI/ML Services**: Communicate asynchronously via non-blocking `WebClient` (Spring WebFlux) with a Python FastAPI microservice.
3. **Feature Extraction**: Pass the core feature subset required by the model:
   * **`amount`**: The transaction value.
   * **`v1` – `v5`**: The first five PCA-transformed feature components extracted from the dataset.
4. **Benchmark & Persist**: Evaluate distributed system performance across experimental strategies:
   * `DISTRIBUTED_AI_SYNCHRONOUS`: Live synchronous AI inference route.
   * `DISTRIBUTED_MOCK_GATEWAY`: Network-baseline mock routing route for isolated overhead analysis.
     Record precise multi-tiered latencies (`executionTimeMs`, parsing time, network communication time, DB write time, serialization time) and decision outcomes to an embedded reactive H2 database.

---

## Tech Stack

* **Java**: 21 (LTS)
* **Framework**: Spring Boot 3.2.12 (Spring WebFlux / Netty reactive event-loop)
* **Python Microservice**: FastAPI, Pydantic, Uvicorn (Fully asynchronous event-loop architecture)
* **Data & Persistence**: Spring Data R2DBC, H2 In-Memory Database (Reactive driver)
* **Validation & Utilities**: Lombok, Jakarta Bean Validation
* **Containerization**: Docker & Docker Compose (Hard resource limits enforced: 3GB RAM, 3 CPU cores)

---

## API Endpoint & Payload Details

### `POST /api/v1/transactions`

Evaluates a transaction payload and returns the risk score, decision status (`APPROVED` / `FLAGGED`), strategy used, processing latency, and granular internal/external telemetry metrics.

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
  "strategy": "DISTRIBUTED_MOCK_GATEWAY"
}