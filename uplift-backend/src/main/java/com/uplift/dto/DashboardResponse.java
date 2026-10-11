package com.uplift.dto;

public record DashboardResponse(
        long totalProjects,
        long openProjects,
        long inProgressProjects,
        long completedProjects,
        long totalProposals,
        long pendingProposals,
        long acceptedProposals,
        long totalUsers
) {
}
