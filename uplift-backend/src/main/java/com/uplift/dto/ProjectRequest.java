package com.uplift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProjectRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 5000) String description,
    String category,
    @DecimalMin(value = "0.00") @Digits(integer = 12, fraction = 2) BigDecimal budgetMin,
    @DecimalMin(value = "0.00") @Digits(integer = 12, fraction = 2) BigDecimal budgetMax
) {}
