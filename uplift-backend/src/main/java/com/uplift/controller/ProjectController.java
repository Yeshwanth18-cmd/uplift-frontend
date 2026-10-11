package com.uplift.controller;

import com.uplift.entity.Project;
import com.uplift.entity.ProjectStatus;
import com.uplift.dto.ProjectRequest;
import com.uplift.dto.ProjectStatusRequest;
import com.uplift.entity.Role;
import com.uplift.entity.User;
import com.uplift.repository.ProjectRepository;
import com.uplift.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // GET ALL PROJECTS
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        // Clients see only their own projects
        if (user.getRole() == Role.CLIENT) {
            return ResponseEntity.ok(
                    projectRepository.findByClientId(user.getId())
            );
        }

        // Freelancers see open projects
        if (user.getRole() == Role.FREELANCER) {
            return ResponseEntity.ok(
                    projectRepository.findByStatus(ProjectStatus.OPEN)
            );
        }

        // Admin sees everything
        return ResponseEntity.ok(projectRepository.findAll());
    }


    // GET PROJECT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        return projectRepository.findById(id)
                .map(project -> {

                    // Admin can access everything
                    if (user.getRole() == Role.ADMIN) {
                        return ResponseEntity.ok(project);
                    }

                    // Client can access own project
                    if (
                            user.getRole() == Role.CLIENT
                                    && project.getClientId().equals(user.getId())
                    ) {
                        return ResponseEntity.ok(project);
                    }

                    // Freelancer can view open projects
                    if (
                            user.getRole() == Role.FREELANCER
                                    && project.getStatus() == ProjectStatus.OPEN
                    ) {
                        return ResponseEntity.ok(project);
                    }

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Project>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // CREATE PROJECT
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        // Only clients can create projects
        if (user.getRole() != Role.CLIENT) {
            throw new AccessDeniedException("Only clients can create projects.");
        }

        validateBudget(request);
        Project project = new Project(
                null,
                request.title().trim(),
                request.description().trim(),
                request.category(),
                request.budgetMin(),
                request.budgetMax(),
                ProjectStatus.OPEN,
                LocalDateTime.now(),
                user.getId()
        );

        Project savedProject = projectRepository.save(project);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProject);
    }


    // UPDATE PROJECT
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest projectDetails,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);
        validateBudget(projectDetails);

        return projectRepository.findById(id)
                .map(project -> {

                    // Admin can update anything
                    boolean isAdmin =
                            user.getRole() == Role.ADMIN;

                    // Client can update only their own project
                    boolean isOwner =
                            user.getRole() == Role.CLIENT
                                    && project.getClientId().equals(user.getId());

                    if (!isAdmin && !isOwner) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .<Project>build();
                    }

                    project.setTitle(projectDetails.title().trim());
                    project.setDescription(projectDetails.description().trim());
                    project.setCategory(projectDetails.category());
                    project.setBudgetMin(projectDetails.budgetMin());
                    project.setBudgetMax(projectDetails.budgetMax());

                    Project updatedProject =
                            projectRepository.save(project);

                    return ResponseEntity.ok(updatedProject);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Transactional
    @PatchMapping("/{id}/status")
    public ResponseEntity<Project> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStatusRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        return projectRepository.findByIdForUpdate(id)
                .map(project -> {
                    boolean isAdmin = user.getRole() == Role.ADMIN;
                    boolean isOwner = user.getRole() == Role.CLIENT
                            && project.getClientId().equals(user.getId());
                    if (!isAdmin && !isOwner) {
                        throw new AccessDeniedException("You cannot update this project.");
                    }

                    boolean validTransition =
                            (project.getStatus() == ProjectStatus.OPEN
                                    && request.status() == ProjectStatus.CANCELLED)
                                    || (project.getStatus() == ProjectStatus.IN_PROGRESS
                                    && request.status() == ProjectStatus.COMPLETED);
                    if (!validTransition) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "The project cannot transition from "
                                        + project.getStatus() + " to " + request.status() + "."
                        );
                    }

                    project.setStatus(request.status());
                    return ResponseEntity.ok(projectRepository.save(project));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // DELETE PROJECT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        return projectRepository.findById(id)
                .map(project -> {

                    boolean isAdmin =
                            user.getRole() == Role.ADMIN;

                    boolean isOwner =
                            user.getRole() == Role.CLIENT
                                    && project.getClientId().equals(user.getId());

                    if (!isAdmin && !isOwner) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .<Void>build();
                    }

                    projectRepository.delete(project);

                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // HELPER METHOD
    private User getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void validateBudget(ProjectRequest request) {
        BigDecimal min = request.budgetMin();
        BigDecimal max = request.budgetMax();
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum budget cannot exceed maximum budget."
            );
        }
    }
}