package com.capstone.aicapstone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public DataSeeder(CompanyRepository companyRepository,
                      StudentRepository studentRepository,
                      ProjectRepository projectRepository,
                      PasswordEncoder passwordEncoder,
                      AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.studentRepository = studentRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public void run(String... args) {
        System.out.println("DataSeeder: app.demo-data.enabled=true detected. Checking sample records...");

        // Sample Company 1
        if (!companyRepository.existsByEmailIgnoreCase("contact@healthtech.io")) {
            Company c1 = new Company();
            c1.setCompanyName("HealthTech AI Labs");
            c1.setEmail("contact@healthtech.io");
            c1.setPassword(passwordEncoder.encode("HealthTech2026!"));
            c1.setIndustryType("Healthcare & Biotechnology");
            c1.setLocation("Boston, MA / Remote");
            c1.setWebsite("https://healthtech.io");
            c1.setDescription("Leading AI research lab developing predictive diagnostics, medical imaging models, and clinical decision support systems.");
            c1.setActive(true);
            companyRepository.save(c1);
        }

        // Sample Company 2
        if (!companyRepository.existsByEmailIgnoreCase("careers@cloudscale.net")) {
            Company c2 = new Company();
            c2.setCompanyName("CloudScale Systems");
            c2.setEmail("careers@cloudscale.net");
            c2.setPassword(passwordEncoder.encode("CloudScale2026!"));
            c2.setIndustryType("Cloud Computing & DevOps");
            c2.setLocation("Seattle, WA / Remote");
            c2.setWebsite("https://cloudscale.net");
            c2.setDescription("Enterprise software company building cloud-native infrastructure, distributed telemetry, and automated Kubernetes orchestration platforms.");
            c2.setActive(true);
            companyRepository.save(c2);
        }

        // Sample Project 1
        saveSampleProjectIfMissing(
                "AI-Powered Medical Image Diagnosis & Pathology Detection",
                "Healthcare & Computer Vision",
                "Advanced",
                "Python, PyTorch, TorchVision, OpenCV, FastAPI, Docker, DICOM",
                "Design and benchmark a deep learning diagnostic pipeline utilizing convolutional neural networks and vision transformers to classify multi-class pathology scans with explainable Grad-CAM heatmaps.",
                "4 Months",
                3,
                "HealthTech AI Labs",
                "contact@healthtech.io"
        );

        // Sample Project 2
        saveSampleProjectIfMissing(
                "Enterprise Cloud-Native Microservices & Kubernetes Platform",
                "Cloud Computing & DevOps",
                "Intermediate",
                "Java, Spring Boot, Docker, Kubernetes, Prometheus, Grafana, PostgreSQL, REST APIs",
                "Architect and deploy a high-throughput, fault-tolerant microservices mesh using Spring Boot and Kubernetes with automated CI/CD and distributed observability.",
                "3 Months",
                4,
                "CloudScale Systems",
                "careers@cloudscale.net"
        );

        auditLogService.log("SYSTEM", "SYSTEM", "SEED_DATA", "Development demo records verified / seeded.");
        System.out.println("DataSeeder: Demo records initialization complete.");
    }

    private void saveSampleProjectIfMissing(String title, String domain, String level, String skills,
                                            String description, String duration, int capacity,
                                            String companyName, String companyEmail) {
        if (!projectRepository.existsByTitleIgnoreCaseAndCompanyEmailIgnoreCase(title, companyEmail)) {
            Project p = new Project();
            p.setTitle(title);
            p.setDomain(domain);
            p.setDifficultyLevel(level);
            p.setRequiredSkills(skills);
            p.setDescription(description);
            p.setDuration(duration);
            p.setNumberOfStudents(capacity);
            p.setCompanyName(companyName);
            p.setCompanyEmail(companyEmail);
            p.setStatus("OPEN");
            projectRepository.save(p);
        }
    }
}
