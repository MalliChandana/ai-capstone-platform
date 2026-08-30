package com.capstone.aicapstone;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Value("${app.admin.email:admin@aicapstone.io}")
    private String adminEmail;

    @Value("${app.admin.password:AdminPass123!}")
    private String adminPassword;

    @Value("${app.admin.name:Platform Administrator}")
    private String adminName;

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRegistrationRepository registrationRepository;
    private final ProjectReportRepository reportRepository;
    private final AuditLogService auditLogService;
    private final PdfExportService pdfExportService;
    private final NotificationService notificationService;

    public AdminController(
            StudentRepository studentRepository,
            CompanyRepository companyRepository,
            ProjectRepository projectRepository,
            ProjectRegistrationRepository registrationRepository,
            ProjectReportRepository reportRepository,
            AuditLogService auditLogService,
            PdfExportService pdfExportService,
            NotificationService notificationService) {

        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.auditLogService = auditLogService;
        this.pdfExportService = pdfExportService;
        this.notificationService = notificationService;
    }

    private boolean isAuthenticatedAdmin(HttpSession session) {
        Object obj = session.getAttribute("admin");
        return obj != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    // =====================================================
    // ADMIN LOGIN / LOGOUT
    // =====================================================

    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {
        if (isAuthenticatedAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLoginSubmit(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if (adminEmail.equalsIgnoreCase(email.trim()) && adminPassword.equals(password)) {
            session.setAttribute("admin", email.trim());
            session.setAttribute("adminName", adminName);
            session.setAttribute("isAdmin", true);
            auditLogService.log(email.trim(), "ADMIN", "LOGIN", "Administrator authenticated successfully");
            return "redirect:/admin/dashboard";
        }

        auditLogService.log(email.trim(), "ADMIN", "LOGIN_FAILED", "Failed administrator login attempt");
        model.addAttribute("error", "Invalid administrator credentials.");
        return "admin-login";
    }

    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        if (isAuthenticatedAdmin(session)) {
            auditLogService.log(adminEmail, "ADMIN", "LOGOUT", "Administrator logged out");
        }
        session.removeAttribute("admin");
        session.removeAttribute("adminName");
        session.removeAttribute("isAdmin");
        return "redirect:/admin/login";
    }

    // =====================================================
    // ADMIN DASHBOARD
    // =====================================================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Student> students = studentRepository.findAll();
        List<Company> companies = companyRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        List<ProjectRegistration> registrations = registrationRepository.findAll();
        List<ProjectReport> reports = reportRepository.findAll();
        List<AuditLog> recentLogs = auditLogService.getRecentLogs().stream().limit(10).collect(Collectors.toList());

        long activeStudents = students.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
        long activeCompanies = companies.stream().filter(c -> Boolean.TRUE.equals(c.getActive())).count();
        long openProjects = projects.stream().filter(p -> "OPEN".equalsIgnoreCase(p.getStatus())).count();
        long pendingApps = registrations.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long acceptedApps = registrations.stream().filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(r.getStatus())).count();
        long completedApps = registrations.stream().filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus())).count();
        long reviewedReports = reports.stream().filter(r -> "REVIEWED".equalsIgnoreCase(r.getStatus())).count();

        model.addAttribute("adminName", session.getAttribute("adminName"));
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("activeStudents", activeStudents);
        model.addAttribute("totalCompanies", companies.size());
        model.addAttribute("activeCompanies", activeCompanies);
        model.addAttribute("totalProjects", projects.size());
        model.addAttribute("openProjects", openProjects);
        model.addAttribute("totalRegistrations", registrations.size());
        model.addAttribute("pendingApps", pendingApps);
        model.addAttribute("acceptedApps", acceptedApps);
        model.addAttribute("completedApps", completedApps);
        model.addAttribute("totalReports", reports.size());
        model.addAttribute("reviewedReports", reviewedReports);
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("recentProjects", projects.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("recentRegistrations", registrations.stream().limit(5).collect(Collectors.toList()));

        return "admin-dashboard";
    }

    // =====================================================
    // STUDENT MANAGEMENT
    // =====================================================

    @GetMapping("/admin/students")
    public String adminStudents(
            @RequestParam(required = false) String search,
            HttpSession session,
            Model model) {

        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Student> students = studentRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            students = students.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                                 (s.getEmail() != null && s.getEmail().toLowerCase().contains(q)) ||
                                 (s.getDepartment() != null && s.getDepartment().toLowerCase().contains(q)) ||
                                 (s.getSkills() != null && s.getSkills().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("students", students);
        model.addAttribute("searchQuery", search);
        return "admin-students";
    }

    @PostMapping("/admin/student/toggle-status/{id}")
    public String toggleStudentStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        studentRepository.findById(id).ifPresent(student -> {
            boolean newStatus = !Boolean.TRUE.equals(student.getActive());
            student.setActive(newStatus);
            studentRepository.save(student);
            auditLogService.log(adminEmail, "ADMIN", "STUDENT_STATUS_TOGGLE",
                    "Student " + student.getEmail() + " status updated to " + (newStatus ? "ACTIVE" : "DEACTIVATED"));
            redirectAttributes.addFlashAttribute("message", "Student account status updated to " + (newStatus ? "Active" : "Deactivated") + ".");
        });

        return "redirect:/admin/students";
    }

    @PostMapping("/admin/student/delete/{id}")
    public String deleteStudent(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        studentRepository.findById(id).ifPresent(student -> {
            registrationRepository.findByStudentId(student.getId()).forEach(registrationRepository::delete);
            studentRepository.delete(student);
            auditLogService.log(adminEmail, "ADMIN", "STUDENT_DELETED", "Student account deleted: " + student.getEmail());
            redirectAttributes.addFlashAttribute("message", "Student account and registrations deleted.");
        });

        return "redirect:/admin/students";
    }

    // =====================================================
    // COMPANY MANAGEMENT
    // =====================================================

    @GetMapping("/admin/companies")
    public String adminCompanies(
            @RequestParam(required = false) String search,
            HttpSession session,
            Model model) {

        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Company> companies = companyRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            companies = companies.stream()
                    .filter(c -> (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(q)) ||
                                 (c.getEmail() != null && c.getEmail().toLowerCase().contains(q)) ||
                                 (c.getIndustryType() != null && c.getIndustryType().toLowerCase().contains(q)) ||
                                 (c.getLocation() != null && c.getLocation().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("companies", companies);
        model.addAttribute("searchQuery", search);
        return "admin-companies";
    }

    @PostMapping("/admin/company/toggle-status/{id}")
    public String toggleCompanyStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        companyRepository.findById(id).ifPresent(company -> {
            boolean newStatus = !Boolean.TRUE.equals(company.getActive());
            company.setActive(newStatus);
            companyRepository.save(company);
            auditLogService.log(adminEmail, "ADMIN", "COMPANY_STATUS_TOGGLE",
                    "Company " + company.getCompanyName() + " (" + company.getEmail() + ") status updated to " + (newStatus ? "ACTIVE" : "DEACTIVATED"));
            redirectAttributes.addFlashAttribute("message", "Company account status updated to " + (newStatus ? "Active" : "Deactivated") + ".");
        });

        return "redirect:/admin/companies";
    }

    @PostMapping("/admin/company/delete/{id}")
    public String deleteCompany(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        companyRepository.findById(id).ifPresent(company -> {
            companyRepository.delete(company);
            auditLogService.log(adminEmail, "ADMIN", "COMPANY_DELETED", "Company deleted: " + company.getCompanyName() + " (" + company.getEmail() + ")");
            redirectAttributes.addFlashAttribute("message", "Company account deleted.");
        });

        return "redirect:/admin/companies";
    }

    // =====================================================
    // PROJECT MANAGEMENT
    // =====================================================

    @GetMapping("/admin/projects")
    public String adminProjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {

        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Project> projects = projectRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            projects = projects.stream()
                    .filter(p -> (p.getTitle() != null && p.getTitle().toLowerCase().contains(q)) ||
                                 (p.getCompanyName() != null && p.getCompanyName().toLowerCase().contains(q)) ||
                                 (p.getRequiredSkills() != null && p.getRequiredSkills().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (domain != null && !domain.trim().isEmpty() && !"ALL".equalsIgnoreCase(domain)) {
            projects = projects.stream()
                    .filter(p -> p.getDomain() != null && p.getDomain().equalsIgnoreCase(domain))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            projects = projects.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        model.addAttribute("projects", projects);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedDomain", domain);
        model.addAttribute("selectedStatus", status);
        return "admin-projects";
    }

    @PostMapping("/admin/project/toggle-status/{id}")
    public String toggleProjectStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        projectRepository.findById(id).ifPresent(project -> {
            String newStatus = "OPEN".equalsIgnoreCase(project.getStatus()) ? "CLOSED" : "OPEN";
            project.setStatus(newStatus);
            projectRepository.save(project);
            auditLogService.log(adminEmail, "ADMIN", "PROJECT_STATUS_MODERATED",
                    "Project '" + project.getTitle() + "' status changed to " + newStatus);
            redirectAttributes.addFlashAttribute("message", "Project status updated to " + newStatus + ".");
        });

        return "redirect:/admin/projects";
    }

    @PostMapping("/admin/project/delete/{id}")
    public String deleteProjectByAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        projectRepository.findById(id).ifPresent(project -> {
            projectRepository.delete(project);
            auditLogService.log(adminEmail, "ADMIN", "PROJECT_REMOVED",
                    "Project removed by administrator: '" + project.getTitle() + "' (" + project.getCompanyName() + ")");
            redirectAttributes.addFlashAttribute("message", "Project removed from platform.");
        });

        return "redirect:/admin/projects";
    }

    // =====================================================
    // APPLICATIONS & PROGRESS MONITORING
    // =====================================================

    @GetMapping("/admin/applications")
    public String adminApplications(
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {

        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<ProjectRegistration> registrations = registrationRepository.findAll();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            registrations = registrations.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        model.addAttribute("registrations", registrations);
        model.addAttribute("selectedStatus", status);
        return "admin-applications";
    }

    // =====================================================
    // REPORTS & EVALUATION
    // =====================================================

    @GetMapping("/admin/reports")
    public String adminReports(HttpSession session, Model model) {
        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<ProjectReport> reports = reportRepository.findAll();
        model.addAttribute("reports", reports);
        return "admin-reports";
    }

    @GetMapping("/admin/report/download/{id}")
    public void adminDownloadReport(
            @PathVariable Long id,
            HttpSession session,
            HttpServletResponse response) {

        if (!isAuthenticatedAdmin(session)) {
            return;
        }

        ProjectReport report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            return;
        }

        try {
            byte[] pdfBytes = pdfExportService.generateProjectReportPdf(report);
            response.setContentType("application/pdf");
            String filename = "Report_" + report.getStudentName().replaceAll("\\s+", "_") + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // AUDIT LOGS
    // =====================================================

    @GetMapping("/admin/audit-logs")
    public String adminAuditLogs(
            @RequestParam(required = false) String role,
            HttpSession session,
            Model model) {

        if (!isAuthenticatedAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<AuditLog> logs = auditLogService.getAllLogs();
        if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
            logs = logs.stream()
                    .filter(l -> l.getActorRole() != null && l.getActorRole().equalsIgnoreCase(role))
                    .collect(Collectors.toList());
        }

        model.addAttribute("logs", logs);
        model.addAttribute("selectedRole", role);
        return "admin-audit-logs";
    }
}
