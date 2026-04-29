package com.example.agent.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {

    private String id;
    private String logEntryId;
    private LocalDateTime analysisTime;

    private String exceptionType;
    private String exceptionMessage;
    private String failingClass;
    private String failingMethod;
    private int failingLine;

    private String rootCauseCategory;
    private String rootCauseDescription;
    private List<ReasoningStep> reasoningChain;
    private double confidenceScore;

    public AnalysisResult() {
        this.reasoningChain = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogEntryId() {
        return logEntryId;
    }

    public void setLogEntryId(String logEntryId) {
        this.logEntryId = logEntryId;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getFailingClass() {
        return failingClass;
    }

    public void setFailingClass(String failingClass) {
        this.failingClass = failingClass;
    }

    public String getFailingMethod() {
        return failingMethod;
    }

    public void setFailingMethod(String failingMethod) {
        this.failingMethod = failingMethod;
    }

    public int getFailingLine() {
        return failingLine;
    }

    public void setFailingLine(int failingLine) {
        this.failingLine = failingLine;
    }

    public String getRootCauseCategory() {
        return rootCauseCategory;
    }

    public void setRootCauseCategory(String rootCauseCategory) {
        this.rootCauseCategory = rootCauseCategory;
    }

    public String getRootCauseDescription() {
        return rootCauseDescription;
    }

    public void setRootCauseDescription(String rootCauseDescription) {
        this.rootCauseDescription = rootCauseDescription;
    }

    public List<ReasoningStep> getReasoningChain() {
        return reasoningChain;
    }

    public void setReasoningChain(List<ReasoningStep> reasoningChain) {
        this.reasoningChain = reasoningChain;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public static class ReasoningStep {
        private int stepOrder;
        private String stepName;
        private String observation;
        private String deduction;
        private String conclusion;

        public ReasoningStep() {
        }

        public ReasoningStep(int stepOrder, String stepName, String observation, String deduction, String conclusion) {
            this.stepOrder = stepOrder;
            this.stepName = stepName;
            this.observation = observation;
            this.deduction = deduction;
            this.conclusion = conclusion;
        }

        public int getStepOrder() {
            return stepOrder;
        }

        public void setStepOrder(int stepOrder) {
            this.stepOrder = stepOrder;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public String getObservation() {
            return observation;
        }

        public void setObservation(String observation) {
            this.observation = observation;
        }

        public String getDeduction() {
            return deduction;
        }

        public void setDeduction(String deduction) {
            this.deduction = deduction;
        }

        public String getConclusion() {
            return conclusion;
        }

        public void setConclusion(String conclusion) {
            this.conclusion = conclusion;
        }

        @Override
        public String toString() {
            return "Step " + stepOrder + " [" + stepName + "]: " + observation + " -> " + deduction + " => " + conclusion;
        }
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "id='" + id + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", rootCauseCategory='" + rootCauseCategory + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", reasoningSteps=" + reasoningChain.size() +
                '}';
    }
}
