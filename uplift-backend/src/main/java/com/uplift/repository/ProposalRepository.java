package com.uplift.repository;

import com.uplift.entity.Proposal;
import com.uplift.entity.ProposalStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    Optional<Proposal> findByProject_IdAndFreelancer_Id(Long projectId, Long freelancerId);

    List<Proposal> findByFreelancer_IdOrderByCreatedAtDesc(Long freelancerId);

    List<Proposal> findByProject_ClientIdOrderByCreatedAtDesc(Long clientId);

    List<Proposal> findByProject_IdOrderByCreatedAtDesc(Long projectId);

    List<Proposal> findByProject_IdAndStatus(Long projectId, ProposalStatus status);

    long countByProject_ClientId(Long clientId);

    long countByProject_ClientIdAndStatus(Long clientId, ProposalStatus status);

    long countByFreelancer_IdAndStatus(Long freelancerId, ProposalStatus status);

    long countByFreelancer_IdAndProject_Status(Long freelancerId, com.uplift.entity.ProjectStatus status);

    long countByFreelancer_Id(Long freelancerId);

    long countByStatus(ProposalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select proposal from Proposal proposal where proposal.id = :id")
    Optional<Proposal> findByIdForUpdate(@Param("id") Long id);
}
