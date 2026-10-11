package com.uplift.repository;

import com.uplift.entity.Project;
import com.uplift.entity.ProjectStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    List<Project> findByClientId(Long clientId);

    List<Project> findByStatus(ProjectStatus status);

    long countByClientId(Long clientId);

    long countByClientIdAndStatus(Long clientId, ProjectStatus status);

    long countByStatus(ProjectStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from Project project where project.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") Long id);
}