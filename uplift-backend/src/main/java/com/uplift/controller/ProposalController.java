package com.uplift.controller;

import com.uplift.dto.ProposalRequest;
import com.uplift.dto.ProposalResponse;
import com.uplift.dto.ProposalStatusRequest;
import com.uplift.service.ProposalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping("/projects/{projectId}/proposals")
    public ResponseEntity<ProposalResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody ProposalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.create(projectId, request, authentication));
    }

    @GetMapping("/proposals/mine")
    public List<ProposalResponse> mine(Authentication authentication) {
        return proposalService.mine(authentication);
    }

    @GetMapping("/proposals/inbox")
    public List<ProposalResponse> inbox(Authentication authentication) {
        return proposalService.inbox(authentication);
    }

    @GetMapping("/projects/{projectId}/proposals")
    public List<ProposalResponse> forProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return proposalService.forProject(projectId, authentication);
    }

    @PatchMapping("/proposals/{proposalId}/status")
    public ProposalResponse updateStatus(
            @PathVariable Long proposalId,
            @Valid @RequestBody ProposalStatusRequest request,
            Authentication authentication
    ) {
        return proposalService.updateStatus(proposalId, request, authentication);
    }
}
