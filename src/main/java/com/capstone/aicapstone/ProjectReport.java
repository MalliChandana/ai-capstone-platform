package com.capstone.aicapstone;

import jakarta.persistence.*;

@Entity
@Table(name = "project_report")
public class ProjectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long registrationId;
    private Long projectId;
    private Long studentId;

    private String studentName;
    private String studentEmail;
    private String projectTitle;

    private String reportTitle;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(columnDefinition = "TEXT")
    private String technologiesUsed;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(columnDefinition = "TEXT")
    private String results;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Column(columnDefinition = "TEXT")
    private String futureScope;

    private String submittedDate;

    @Column(columnDefinition = "TEXT")
    private String companyFeedback;

    private String feedbackDate;
    private String scoreOrGrade;
    private String status; // SUBMITTED, REVIEWED, APPROVED

    public ProjectReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getTechnologiesUsed() {
        return technologiesUsed;
    }

    public void setTechnologiesUsed(String technologiesUsed) {
        this.technologiesUsed = technologiesUsed;
    }

    public String getMethodology() {
        return methodology;
    }

    public void setMethodology(String methodology) {
        this.methodology = methodology;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getFutureScope() {
        return futureScope;
    }

    public void setFutureScope(String futureScope) {
        this.futureScope = futureScope;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(String submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getCompanyFeedback() {
        return companyFeedback;
    }

    public void setCompanyFeedback(String companyFeedback) {
        this.companyFeedback = companyFeedback;
    }

    public String getFeedbackDate() {
        return feedbackDate;
    }

    public void setFeedbackDate(String feedbackDate) {
        this.feedbackDate = feedbackDate;
    }

    public String getScoreOrGrade() {
        return scoreOrGrade;
    }

    public void setScoreOrGrade(String scoreOrGrade) {
        this.scoreOrGrade = scoreOrGrade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
