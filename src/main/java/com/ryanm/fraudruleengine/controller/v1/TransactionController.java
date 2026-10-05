package com.ryanm.fraudruleengine.controller.v1;

import com.ryanm.fraudruleengine.controller.v1.dto.TransactionRequest;
import com.ryanm.fraudruleengine.controller.v1.dto.TransactionResponse;
import com.ryanm.fraudruleengine.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Submit transactions for fraud evaluation")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(final TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(
            summary = "Evaluate a transaction",
            description = "Runs all fraud rules in parallel and returns a risk decision. "
                    + "Transactions are persisted as PENDING, then updated to ALLOWED / FLAGGED / BLOCKED.")
    public ResponseEntity<TransactionResponse> evaluate(@Valid @RequestBody final TransactionRequest request) {
        final TransactionResponse response = transactionService.evaluate(request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + response.transactionId()))
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }
}
