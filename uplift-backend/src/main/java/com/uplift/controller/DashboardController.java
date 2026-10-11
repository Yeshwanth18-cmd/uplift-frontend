package com.uplift.controller;

import com.uplift.dto.DashboardResponse;
import com.uplift.entity.ProjectStatus;
import com.uplift.entity.ProposalStatus;
import com.uplift.entity.Role;
import com.uplift.entity.User;
import com.uplift.repository.ProjectRepository;
import com.uplift.repository.ProposalRepository;
import com.uplift.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;

    public DashboardController(
            ProjectRepository projectRepository,
            ProposalRepository proposalRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public DashboardResponse summary(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (user.getRole() == Role.CLIENT) {
            return new DashboardResponse(
                    projectRepository.countByClientId(user.getId()),
                    projectRepository.countByClientIdAndStatus(user.getId(), ProjectStatus.OPEN),
                    projectRepository.countByClientIdAndStatus(user.getId(), ProjectStatus.IN_PROGRESS),
                    projectRepository.countByClientIdAndStatus(user.getId(), ProjectStatus.COMPLETED),
                    proposalRepository.countByProject_ClientId(user.getId()),
                    proposalRepository.countByProject_ClientIdAndStatus(user.getId(), ProposalStatus.PENDING),
                    proposalRepository.countByProject_ClientIdAndStatus(user.getId(), ProposalStatus.ACCEPTED),
                    0
            );
        }

        if (user.getRole() == Role.FREELANCER) {
            return new DashboardResponse(
                    projectRepository.countByStatus(ProjectStatus.OPEN),
                    projectRepository.countByStatus(ProjectStatus.OPEN),
                    proposalRepository.countByFreelancer_IdAndProject_Status(user.getId(), ProjectStatus.IN_PROGRESS),
                    proposalRepository.countByFreelancer_IdAndProject_Status(user.getId(), ProjectStatus.COMPLETED),
                    proposalRepository.countByFreelancer_Id(user.getId()),
                    proposalRepository.countByFreelancer_IdAndStatus(user.getId(), ProposalStatus.PENDING),
                    proposalRepository.countByFreelancer_IdAndStatus(user.getId(), ProposalStatus.ACCEPTED),
                    0
            );
        }

        return new DashboardResponse(
                projectRepository.count(),
                projectRepository.countByStatus(ProjectStatus.OPEN),
                projectRepository.countByStatus(ProjectStatus.IN_PROGRESS),
                projectRepository.countByStatus(ProjectStatus.COMPLETED),
                proposalRepository.count(),
                proposalRepository.countByStatus(ProposalStatus.PENDING),
                proposalRepository.countByStatus(ProposalStatus.ACCEPTED),
                userRepository.count()
        );
    }
}
