package com.uplift.service;

import com.uplift.dto.ProposalRequest;
import com.uplift.dto.ProposalResponse;
import com.uplift.dto.ProposalStatusRequest;
import com.uplift.entity.Project;
import com.uplift.entity.ProjectStatus;
import com.uplift.entity.Proposal;
import com.uplift.entity.ProposalStatus;
import com.uplift.entity.Role;
import com.uplift.entity.User;
import com.uplift.repository.ProjectRepository;
import com.uplift.repository.ProposalRepository;
import com.uplift.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProposalService(
            ProposalRepository proposalRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProposalResponse create(
            Long projectId,
            ProposalRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUser(authentication);
        if (user.getRole() != Role.FREELANCER) {
            throw new AccessDeniedException("Only freelancers can submit proposals.");
        }

        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This project is no longer accepting proposals.");
        }
        if (proposalRepository.findByProject_IdAndFreelancer_Id(projectId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted a proposal for this project.");
        }

        Proposal proposal = proposalRepository.save(new Proposal(
                project,
                user,
                request.amount(),
                request.deliveryDays(),
                request.coverLetter().trim()
        ));
        return toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> mine(Authentication authentication) {
        User user = authenticatedUser(authentication);
        if (user.getRole() != Role.FREELANCER) {
            throw new AccessDeniedException("Only freelancers can view their proposals.");
        }
        return proposalRepository.findByFreelancer_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> inbox(Authentication authentication) {
        User user = authenticatedUser(authentication);
        if (user.getRole() != Role.CLIENT) {
            throw new AccessDeniedException("Only clients can view their proposal inbox.");
        }
        return proposalRepository.findByProject_ClientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> forProject(Long projectId, Authentication authentication) {
        User user = authenticatedUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
        if (user.getRole() != Role.ADMIN
                && (user.getRole() != Role.CLIENT || !project.getClientId().equals(user.getId()))) {
            throw new AccessDeniedException("You cannot view proposals for this project.");
        }
        return proposalRepository.findByProject_IdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProposalResponse updateStatus(
            Long proposalId,
            ProposalStatusRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUser(authentication);
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        Project project = projectRepository.findByIdForUpdate(proposal.getProject().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
        if (user.getRole() != Role.ADMIN
                && (user.getRole() != Role.CLIENT || !project.getClientId().equals(user.getId()))) {
            throw new AccessDeniedException("You cannot update this proposal.");
        }
        if (request.status() == ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A proposal cannot be moved back to pending.");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This proposal has already been decided.");
        }
        if (request.status() == ProposalStatus.ACCEPTED) {
            if (project.getStatus() != ProjectStatus.OPEN) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This project already has an active proposal.");
            }
            project.setStatus(ProjectStatus.IN_PROGRESS);
            proposalRepository.findByProject_IdAndStatus(project.getId(), ProposalStatus.PENDING)
                    .stream()
                    .filter(other -> !other.getId().equals(proposal.getId()))
                    .forEach(other -> other.setStatus(ProposalStatus.REJECTED));
        }
        proposal.setStatus(request.status());
        return toResponse(proposal);
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ProposalResponse toResponse(Proposal proposal) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getProject().getId(),
                proposal.getProject().getTitle(),
                proposal.getFreelancer().getId(),
                proposal.getFreelancer().getName(),
                proposal.getFreelancer().getEmail(),
                proposal.getAmount(),
                proposal.getDeliveryDays(),
                proposal.getCoverLetter(),
                proposal.getStatus(),
                proposal.getCreatedAt()
        );
    }
}
