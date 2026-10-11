package com.uplift.dto;

import com.uplift.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusRequest(@NotNull ProjectStatus status) {
}
