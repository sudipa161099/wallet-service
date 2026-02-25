package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<String> transfer(@Valid @RequestBody TransferRequest request) {

        walletService.transfer(
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmount()
        );

        return ResponseEntity.ok("Transfer successful");
    }
}