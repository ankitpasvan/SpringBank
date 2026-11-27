package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.banking.dto.ExchangeRateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);
    private static final BigDecimal DEMO_FEE_RATE = new BigDecimal("0.0049");

    // Fallback: 1 INR = X currency. 1 USD = 92.91 INR (verified 27 Nov 2026).
    private static final Map<String, BigDecimal> FALLBACK_PER_INR = Map.of(
            "INR", BigDecimal.ONE,
            "USD", new BigDecimal("0.010763"),
            "EUR", new BigDecimal("0.009250"),
            "GBP", new BigDecimal("0.008050"),
            "AUD", new BigDecimal("0.016100"),
            "CAD", new BigDecimal("0.014800"),
            "SGD", new BigDecimal("0.013900")
    );

    private static final List<String> SUPPORTED = List.of("INR", "USD", "EUR", "GBP", "AUD", "CAD", "SGD");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    public List<String> supportedCurrencies() {
        return SUPPORTED;
    }

    public ExchangeRateResponse getRates(String base, List<String> symbols) {
        String cleanBase = normalise(base);
        List<String> cleanSymbols = symbols == null || symbols.isEmpty()
                ? SUPPORTED.stream().filter(c -> !c.equals(cleanBase)).toList()
                : symbols.stream().map(this::normalise).filter(SUPPORTED::contains).toList();
        String key = cleanBase + ":" + String.join(",", cleanSymbols);
        CachedRates cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return new ExchangeRateResponse(cleanBase, cached.rates(), cached.fetchedAt(), cached.fallback(), cached.source());
        }
        ExchangeRateResponse fresh = fetchLive(cleanBase, cleanSymbols);
        cache.put(key, new CachedRates(fresh.rates(), fresh.fetchedAt(), fresh.fallback(), fresh.source(), Instant.now().plus(CACHE_TTL)));
        return fresh;
    }

    public BigDecimal getRate(String from, String to) {
        String cleanFrom = normalise(from);
        String cleanTo = normalise(to);
        if (cleanFrom.equals(cleanTo)) {
            return BigDecimal.ONE;
        }
        ExchangeRateResponse response = getRates(cleanFrom, List.of(cleanTo));
        BigDecimal rate = response.rates().get(cleanTo);
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency pair: " + cleanFrom + " -> " + cleanTo);
        }
        return rate;
    }

    public BigDecimal feeFor(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(DEMO_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private ExchangeRateResponse fetchLive(String base, List<String> symbols) {
        try {
            String url = "https://api.frankfurter.dev/v1/latest?base=" + base
                    + "&symbols=" + String.join(",", symbols);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode ratesNode = root.get("rates");
                Map<String, BigDecimal> rates = new LinkedHashMap<>();
                for (String symbol : symbols) {
                    if (ratesNode != null && ratesNode.has(symbol)) {
                        rates.put(symbol, new BigDecimal(ratesNode.get(symbol).asText()));
                    }
                }
                if (!rates.isEmpty()) {
                    return new ExchangeRateResponse(base, rates, Instant.now(), false, "Frankfurter / ECB");
                }
            }
            log.warn("Frankfurter returned status {}", response.statusCode());
        } catch (Exception e) {
            log.warn("Live FX fetch failed, using fallback rates: {}", e.getMessage());
        }
        return fallbackResponse(base, symbols);
    }

    private ExchangeRateResponse fallbackResponse(String base, List<String> symbols) {
        BigDecimal basePerInr = FALLBACK_PER_INR.getOrDefault(base, BigDecimal.ONE);
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (String symbol : symbols) {
            BigDecimal targetPerInr = FALLBACK_PER_INR.getOrDefault(symbol, BigDecimal.ONE);
            rates.put(symbol, targetPerInr.divide(basePerInr, 6, RoundingMode.HALF_UP));
        }
        return new ExchangeRateResponse(base, rates, Instant.now(), true, "Demo fallback (indicative)");
    }

    private String normalise(String code) {
        if (code == null || code.isBlank()) {
            return "INR";
        }
        String clean = code.trim().toUpperCase();
        return SUPPORTED.contains(clean) ? clean : "INR";
    }

    private record CachedRates(Map<String, BigDecimal> rates, Instant fetchedAt, boolean fallback, String source, Instant expiresAt) {
    }
}