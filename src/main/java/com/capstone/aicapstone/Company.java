package com.capstone.aicapstone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "industry_type")
    private String industryType;

    @Column(name = "location")
    private String location;

    @Column(name = "website")
    private String website;

    @Column(name = "active")
    private Boolean active = true;

    // ==========================================
    // DEFAULT CONSTRUCTOR
    // ==========================================

    public Company() {
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    // ==========================================
    // GETTER AND SETTER FOR ID
    // ==========================================

    public Long getId() {
        return id;
    }


    // ==========================================
    // GETTER AND SETTER FOR COMPANY NAME
    // ==========================================

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    // ==========================================
    // GETTER AND SETTER FOR EMAIL
    // ==========================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // ==========================================
    // GETTER AND SETTER FOR PASSWORD
    // ==========================================

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    // ==========================================
    // GETTER AND SETTER FOR DESCRIPTION
    // ==========================================

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    // ==========================================
    // GETTER AND SETTER FOR INDUSTRY TYPE
    // ==========================================

    public String getIndustryType() {
        return industryType;
    }

    public void setIndustryType(String industryType) {
        this.industryType = industryType;
    }

    public String getIndustry() {
        return industryType;
    }

    public void setIndustry(String industry) {
        this.industryType = industry;
    }


    // ==========================================
    // GETTER AND SETTER FOR LOCATION
    // ==========================================

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    // ==========================================
    // GETTER AND SETTER FOR WEBSITE
    // ==========================================

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}