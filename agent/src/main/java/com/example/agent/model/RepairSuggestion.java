package com.example.agent.model;

import java.time.LocalDateTime;

public class RepairSuggestion {

    private String id;
    private String analysisResultId;
    private LocalDateTime generatedTime;

    private String title;
    private String description;
    private String severity;
    private String fixType;
    private String codePatch;
    private String referenceDoc;

    private boolean notificationSent;

    public RepairSuggestion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalysisResultId() {
        return analysisResultId;
    }

    public void setAnalysisResultId(String analysisResultId) {
        this.analysisResultId = analysisResultId;
    }

    public LocalDateTime getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(LocalDateTime generatedTime) {
        this.generatedTime = generatedTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getFixType() {
        return fixType;
    }

    public void setFixType(String fixType) {
        this.fixType = fixType;
    }

    public String getCodePatch() {
        return codePatch;
    }

    public void setCodePatch(String codePatch) {
        this.codePatch = codePatch;
    }

    public String getReferenceDoc() {
        return referenceDoc;
    }

    public void setReferenceDoc(String referenceDoc) {
        this.referenceDoc = referenceDoc;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    @Override
    public String toString() {
        return "RepairSuggestion{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", severity='" + severity + '\'' +
                ", fixType='" + fixType + '\'' +
                ", notificationSent=" + notificationSent +
                '}';
    }
}
