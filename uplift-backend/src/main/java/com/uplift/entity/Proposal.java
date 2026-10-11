package com.uplift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "proposals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_proposal_project_freelancer",
                columnNames = {"project_id", "freelancer_id"}
        )
)
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer deliveryDays;

    @Column(nullable = false, length = 3000)
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Proposal() {
    }

    public Proposal(
            Project project,
            User freelancer,
            BigDecimal amount,
            Integer deliveryDays,
            String coverLetter
    ) {
        this.project = project;
        this.freelancer = freelancer;
        this.amount = amount;
        this.deliveryDays = deliveryDays;
        this.coverLetter = coverLetter;
        this.status = ProposalStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public User getFreelancer() {
        return freelancer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
