package com.audit.transaction_service.controller;

import com.audit.transaction_service.dto.RequestDto;
import com.audit.transaction_service.dto.ResponseDto;
import com.audit.transaction_service.repository.TransactionRepository;
import com.audit.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// combination of @Controller and @ResponseBody tells springboot that the class handles http web requests and returns into JSON format
@RestController
// Sets the base URL path for all endpoints inside this class. Any request sent to
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    // Declares a reference to service layer
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Specifies that this method only responds to HTTP POST requests sent to /api/v1/transactions.
    @PostMapping
    // : Takes the incoming raw JSON string in the request body and automatically maps it into a JAVA RequestDto object
    public ResponseEntity<ResponseDto> processTransaction(@Valid @RequestBody RequestDto request) {
        ResponseDto response = transactionService.processTransaction(request);
        // Wraps ResponseDto in HTTP response wrapper with HTTP 200 OK status code, converting Java ResponseDto back into JSON for client
        return ResponseEntity.ok(response);
    }

}
