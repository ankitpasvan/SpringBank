package com.example.banking.controller;

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

import com.example.banking.dto.BeneficiaryRequest;
import com.example.banking.dto.BeneficiaryResponse;
import com.example.banking.service.BeneficiaryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public List<BeneficiaryResponse> list(Principal principal) {
        return beneficiaryService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse create(Principal principal, @Valid @RequestBody BeneficiaryRequest request) {
        return beneficiaryService.create(principal.getName(), request);
    }

    @PostMapping("/{id}/verify")
    public BeneficiaryResponse verify(Principal principal, @PathVariable("id") String id) {
        return beneficiaryService.verify(principal.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable("id") String id) {
        beneficiaryService.delete(principal.getName(), id);
    }
}