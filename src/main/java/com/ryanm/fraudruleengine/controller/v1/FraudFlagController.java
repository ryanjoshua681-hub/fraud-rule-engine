package com.ryanm.fraudruleengine.controller.v1;

import com.ryanm.fraudruleengine.controller.v1.dto.FraudFlagResponse;
import com.ryanm.fraudruleengine.service.FraudFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-flags")
@Tag(name = "Fraud Flags", description = "Retrieve fraud evaluation results")
public class FraudFlagController {

    private final FraudFlagService fraudFlagService;

    public FraudFlagController(final FraudFlagService fraudFlagService) {
        this.fraudFlagService = fraudFlagService;
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(
            summary = "Get fraud flag by transaction ID",
            description = "Returns the risk score, risk level, action taken, and the list of rules that triggered for a given transaction.")
    public ResponseEntity<FraudFlagResponse> findByTransactionId(
            @PathVariable final UUID transactionId) {
        return ResponseEntity.ok(fraudFlagService.findByTransactionId(transactionId));
    }
}
