package com.example.banking.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.PotRequest;
import com.example.banking.dto.PotResponse;
import com.example.banking.service.SavingsPotService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/pots")
public class SavingsPotController {

    private final SavingsPotService potService;

    public SavingsPotController(SavingsPotService potService) {
        this.potService = potService;
    }

    @GetMapping
    public List<PotResponse> list(Principal principal) {
        return potService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PotResponse create(Principal principal, @Valid @RequestBody PotRequest request) {
        return potService.create(principal.getName(), request);
    }

    @PostMapping("/{id}/add")
    public PotResponse addMoney(Principal principal, @PathVariable("id") String id,
                                @Valid @RequestBody MoveRequest request) {
        return potService.addMoney(principal.getName(), id, request.accountId(), request.amount());
    }

    @PostMapping("/{id}/withdraw")
    public PotResponse withdrawMoney(Principal principal, @PathVariable("id") String id,
                                     @Valid @RequestBody MoveRequest request) {
        return potService.withdrawMoney(principal.getName(), id, request.accountId(), request.amount());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable("id") String id) {
        potService.delete(principal.getName(), id);
    }

    public record MoveRequest(
            @NotBlank(message = "Account id is required") String accountId,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount
    ) {
    }
}