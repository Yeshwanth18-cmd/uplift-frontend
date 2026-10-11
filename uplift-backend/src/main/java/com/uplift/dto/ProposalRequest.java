package com.uplift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProposalRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2)
        BigDecimal amount,
        @NotNull @Positive Integer deliveryDays,
        @NotBlank @Size(max = 3000) String coverLetter
) {
}
