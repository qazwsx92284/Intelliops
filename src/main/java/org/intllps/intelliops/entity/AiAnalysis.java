package org.intllps.intelliops.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String summary;

    @NotBlank
    @Column(name = "suspected_root_cause", nullable = false, length = 4000)
    private String suspectedRootCause;

    @NotBlank
    @Column(name = "supporting_evidence", nullable = false, length = 4000)
    private String supportingEvidence;

    @NotBlank
    @Column(name = "potential_impact", nullable = false, length = 2000)
    private String potentialImpact;

    @NotBlank
    @Column(name = "recommended_troubleshooting_steps", nullable = false, length = 4000)
    private String recommendedTroubleshootingSteps;

    @NotBlank
    @Column(name = "confidence_level", nullable = false, length = 50)
    private String confidenceLevel;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "raw_response", length = 8000)
    private String rawResponse;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiAnalysis() {
    }

    public AiAnalysis(
            Incident incident,
            String summary,
            String suspectedRootCause,
            String supportingEvidence,
            String potentialImpact,
            String recommendedTroubleshootingSteps,
            String confidenceLevel,
            String modelName,
            String rawResponse
    ) {
        this.incident = incident;
        this.summary = summary;
        this.suspectedRootCause = suspectedRootCause;
        this.supportingEvidence = supportingEvidence;
        this.potentialImpact = potentialImpact;
        this.recommendedTroubleshootingSteps = recommendedTroubleshootingSteps;
        this.confidenceLevel = confidenceLevel;
        this.modelName = modelName;
        this.rawResponse = rawResponse;
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSuspectedRootCause() {
        return suspectedRootCause;
    }

    public void setSuspectedRootCause(String suspectedRootCause) {
        this.suspectedRootCause = suspectedRootCause;
    }

    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence(String supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }

    public String getPotentialImpact() {
        return potentialImpact;
    }

    public void setPotentialImpact(String potentialImpact) {
        this.potentialImpact = potentialImpact;
    }

    public String getRecommendedTroubleshootingSteps() {
        return recommendedTroubleshootingSteps;
    }

    public void setRecommendedTroubleshootingSteps(String recommendedTroubleshootingSteps) {
        this.recommendedTroubleshootingSteps = recommendedTroubleshootingSteps;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

