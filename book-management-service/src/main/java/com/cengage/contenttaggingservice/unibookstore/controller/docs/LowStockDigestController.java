package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RequestMapping("/api/v1/books/low-stock-digests")
@Validated
@Tag(name = "Low-stock digests", description = "Stored low-stock reports for catalogue administrators")
public interface LowStockDigestController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List recent low-stock digests", description = "Returns stored digests, newest first.")
    List<LowStockDigestResponse> recentDigests(
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate a low-stock digest now",
            description = "Computes and stores a digest on demand, in addition to the scheduled runs.")
    LowStockDigestResponse generateDigest(
            @RequestParam(name = "threshold", defaultValue = "10") @Min(0) int threshold,
            @RequestParam(name = "onlyEnabled", defaultValue = "true") boolean onlyEnabled);
}
