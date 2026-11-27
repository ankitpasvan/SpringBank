package com.example.banking.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.ExchangeRateResponse;
import com.example.banking.service.ExchangeRateService;

@RestController
@RequestMapping("/api/rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public ResponseEntity<ExchangeRateResponse> rates(
            @RequestParam(defaultValue = "INR") String base,
            @RequestParam(required = false) List<String> symbols) {
        return ResponseEntity.ok(exchangeRateService.getRates(base, symbols));
    }

    @GetMapping("/convert")
    public ResponseEntity<ConvertResponse> convert(
            @RequestParam(defaultValue = "INR") String from,
            @RequestParam(defaultValue = "USD") String to,
            @RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        BigDecimal rate = exchangeRateService.getRate(from, to);
        BigDecimal feeInSource = exchangeRateService.feeFor(amount);
        BigDecimal amountAfterFee = amount.subtract(feeInSource);
        BigDecimal recipientGets = amountAfterFee.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return ResponseEntity.ok(new ConvertResponse(
                from.toUpperCase(), to.toUpperCase(), amount, rate,
                feeInSource, recipientGets, "DEMO — indicative rate, no real money moves"));
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<String>> currencies() {
        return ResponseEntity.ok(exchangeRateService.supportedCurrencies());
    }

    public record ConvertResponse(
            String from, String to, BigDecimal amount, BigDecimal rate,
            BigDecimal fee, BigDecimal recipientGets, String note) {
    }
}