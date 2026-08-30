package com.capstone.aicapstone;

import jakarta.persistence.*;

@Entity
public class ProjectRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private Long projectId;

    private String studentName;

    private String studentEmail;

    private String projectTitle;

    private String companyEmail;

    private String registrationDate;

    private String status;

    // ================================
    // PROGRESS TRACKING
    // ================================

    private int progress;

    private String currentMilestone;

    private String milestoneStatus;

    @Column(columnDefinition = "TEXT")
    private String studentRemarks;


    // ================================
    // CONSTRUCTOR
    // ================================

    public ProjectRegistration() {
    }


    // ================================
    // ID
    // ================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // ================================
    // STUDENT ID
    // ================================

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }


    // ================================
    // PROJECT ID
    // ================================

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }


    // ================================
    // STUDENT NAME
    // ================================

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }


    // ================================
    // STUDENT EMAIL
    // ================================

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }


    // ================================
    // PROJECT TITLE
    // ================================

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }


    // ================================
    // REGISTRATION STATUS
    // ================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // ================================
    // PROGRESS
    // ================================

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }


    // ================================
    // CURRENT MILESTONE
    // ================================

    public String getCurrentMilestone() {
        return currentMilestone;
    }

    public void setCurrentMilestone(String currentMilestone) {
        this.currentMilestone = currentMilestone;
    }


    // ================================
    // MILESTONE STATUS
    // ================================

    public String getMilestoneStatus() {
        return milestoneStatus;
    }

    public void setMilestoneStatus(String milestoneStatus) {
        this.milestoneStatus = milestoneStatus;
    }


    // ================================
    // STUDENT REMARKS
    // ================================

    public String getStudentRemarks() {
        return studentRemarks;
    }

    public void setStudentRemarks(String studentRemarks) {
        this.studentRemarks = studentRemarks;
    }
}