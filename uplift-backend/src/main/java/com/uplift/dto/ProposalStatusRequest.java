package com.uplift.dto;

import com.uplift.entity.ProposalStatus;
import jakarta.validation.constraints.NotNull;

public record ProposalStatusRequest(@NotNull ProposalStatus status) {
}
