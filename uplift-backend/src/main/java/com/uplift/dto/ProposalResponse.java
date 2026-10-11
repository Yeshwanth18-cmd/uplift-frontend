package com.uplift.dto;

import com.uplift.entity.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProposalResponse(
        Long id,
        Long projectId,
        String projectTitle,
        Long freelancerId,
        String freelancerName,
        String freelancerEmail,
        BigDecimal amount,
        Integer deliveryDays,
        String coverLetter,
        ProposalStatus status,
        LocalDateTime createdAt
) {
}
