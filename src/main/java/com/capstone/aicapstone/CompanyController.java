package com.capstone.aicapstone;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRegistrationRepository registrationRepository;
    private final ProjectReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final PdfExportService pdfExportService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CompanyController(
            CompanyRepository companyRepository,
            ProjectRepository projectRepository,
            ProjectRegistrationRepository registrationRepository,
            ProjectReportRepository reportRepository,
            StudentRepository studentRepository,
            NotificationService notificationService,
            PdfExportService pdfExportService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {

        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
        this.pdfExportService = pdfExportService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    private Company getAuthenticatedCompany(HttpSession session) {
        Object obj = session.getAttribute("company");
        if (obj instanceof Company) {
            return (Company) obj;
        }
        return null;
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @GetMapping("/company/login")
    public String companyLogin(HttpSession session) {
        if (getAuthenticatedCompany(session) != null) {
            return "redirect:/company/dashboard";
        }
        return "company-login";
    }

    @PostMapping("/company/login")
    public String companyLoginSubmit(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Company> opt = companyRepository.findByEmail(email.trim());
        if (opt.isEmpty()) {
            auditLogService.log(email.trim(), "COMPANY", "LOGIN_FAILED", "Company login failed: email not found");
            model.addAttribute("error", "No company registered with this email address.");
            return "company-login";
        }

        Company company = opt.get();
        if (Boolean.FALSE.equals(company.getActive())) {
            auditLogService.log(email.trim(), "COMPANY", "LOGIN_BLOCKED", "Company login blocked: account deactivated by administrator");
            model.addAttribute("error", "Your organization account has been deactivated. Please contact the administrator.");
            return "company-login";
        }

        boolean passwordMatch = false;
        if (passwordEncoder.matches(password, company.getPassword())) {
            passwordMatch = true;
        } else if (company.getPassword().equals(password)) {
            // Upgrade plaintext password to BCrypt hash transparently
            company.setPassword(passwordEncoder.encode(password));
            companyRepository.save(company);
            passwordMatch = true;
        }

        if (!passwordMatch) {
            auditLogService.log(email.trim(), "COMPANY", "LOGIN_FAILED", "Company login failed: incorrect password");
            model.addAttribute("error", "Incorrect password. Please try again.");
            return "company-login";
        }

        session.setAttribute("company", company);
        auditLogService.log(company.getEmail(), "COMPANY", "LOGIN", "Company logged in successfully: " + company.getCompanyName());
        return "redirect:/company/dashboard";
    }

    // =====================================================
    // REGISTRATION
    // =====================================================

    @GetMapping("/company/register")
    public String companyRegister(HttpSession session) {
        if (getAuthenticatedCompany(session) != null) {
            return "redirect:/company/dashboard";
        }
        return "company-register";
    }

    @PostMapping("/company/register")
    public String companyRegisterSubmit(
            Company company,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (company.getEmail() == null || company.getEmail().trim().isEmpty() ||
            company.getPassword() == null || company.getPassword().trim().isEmpty() ||
            company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            model.addAttribute("error", "Please fill in all required company fields.");
            return "company-register";
        }

        if (companyRepository.findByEmail(company.getEmail().trim()).isPresent()) {
            model.addAttribute("error", "A company with email '" + company.getEmail() + "' already exists. Please log in.");
            return "company-register";
        }

        company.setEmail(company.getEmail().trim());
        company.setPassword(passwordEncoder.encode(company.getPassword()));
        company.setActive(true);
        companyRepository.save(company);

        auditLogService.log(company.getEmail(), "COMPANY", "REGISTER", "New company registered: " + company.getCompanyName());

        // Send welcome notification
        notificationService.notifyCompany(company.getEmail(), "Welcome to AI Capstone Platform",
                "Your organization account is ready. Post your first capstone project to receive applications!",
                "/company/project/post");

        redirectAttributes.addFlashAttribute("message", "Company registered successfully! Please log in.");
        return "redirect:/company/login";
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    @GetMapping("/company/dashboard")
    public String companyDashboard(
            @RequestParam(required = false) String email,
            HttpSession session,
            Model model) {

        Company company = getAuthenticatedCompany(session);
        if (company == null && email != null) {
            company = companyRepository.findByEmail(email).orElse(null);
            if (company != null) {
                session.setAttribute("company", company);
            }
        }

        if (company == null) {
            return "redirect:/company/login";
        }

        // Refresh
        company = companyRepository.findById(company.getId()).orElse(company);
        session.setAttribute("company", company);

        final String compEmail = company.getEmail();
        List<Project> companyProjects = projectRepository.findByCompanyEmail(compEmail);
        Set<Long> projectIds = companyProjects.stream().map(Project::getId).collect(Collectors.toSet());

        List<ProjectRegistration> allRegistrations = registrationRepository.findAll().stream()
                .filter(r -> projectIds.contains(r.getProjectId()) || (r.getCompanyEmail() != null && r.getCompanyEmail().equalsIgnoreCase(compEmail)))
                .collect(Collectors.toList());

        long totalProjects = companyProjects.size();
        long totalApplications = allRegistrations.size();
        long pendingApplications = allRegistrations.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long acceptedStudents = allRegistrations.stream().filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(r.getStatus())).count();
        long completedProjects = allRegistrations.stream().filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus())).count();
        long activeProjects = companyProjects.stream().filter(p -> "OPEN".equalsIgnoreCase(p.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(p.getStatus())).count();

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("acceptedStudents", acceptedStudents);
        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("activeProjects", activeProjects);
        model.addAttribute("recentProjects", companyProjects.stream().limit(3).collect(Collectors.toList()));
        model.addAttribute("recentApplications", allRegistrations.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-dashboard";
    }

    // =====================================================
    // POST PROJECT
    // =====================================================

    @GetMapping("/company/project/post")
    public String showProjectPostPage(HttpSession session, Model model) {
        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());
        model.addAttribute("company", company);
        model.addAttribute("project", new Project());
        model.addAttribute("unreadNotifications", unreadNotifications);
        return "company-project-post";
    }

    @PostMapping("/company/project/post")
    public String postProject(
            Project project,
            @RequestParam(required = false) String projectTitle,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        if (projectTitle != null && !projectTitle.trim().isEmpty() && (project.getTitle() == null || project.getTitle().trim().isEmpty())) {
            project.setTitle(projectTitle);
        }

        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Project title is required.");
            return "redirect:/company/project/post";
        }

        String trimmedTitle = project.getTitle().trim();
        project.setTitle(trimmedTitle);

        // Prevent duplicate project postings with the exact same title by the same company
        if (projectRepository.existsByTitleIgnoreCaseAndCompanyEmailIgnoreCase(trimmedTitle, company.getEmail().trim())) {
            redirectAttributes.addFlashAttribute("error", "A capstone project with the title '" + trimmedTitle + "' already exists in your organization's portfolio. You can edit it under My Projects.");
            return "redirect:/company/project/post";
        }

        project.setCompanyId(company.getId());
        project.setCompanyName(company.getCompanyName());
        project.setCompanyEmail(company.getEmail());
        if (project.getStatus() == null || project.getStatus().trim().isEmpty()) {
            project.setStatus("OPEN");
        }

        projectRepository.save(project);

        notificationService.notifyCompany(company.getEmail(), "Project Published",
                "Your project '" + project.getTitle() + "' is now live for students to apply.", "/company/projects");

        redirectAttributes.addFlashAttribute("message", "Capstone project published successfully!");
        return "redirect:/company/projects";
    }

    // =====================================================
    // VIEW COMPANY PROJECTS
    // =====================================================

    @GetMapping("/company/projects")
    public String projects(HttpSession session, Model model) {
        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        List<Project> companyProjects = projectRepository.findByCompanyEmail(company.getEmail());
        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("projects", companyProjects);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-projects";
    }

    // =====================================================
    // EDIT & DELETE PROJECT
    // =====================================================

    @GetMapping("/company/project/edit/{id}")
    public String editProjectPage(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null || !project.getCompanyEmail().equalsIgnoreCase(company.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Project not found or unauthorized.");
            return "redirect:/company/projects";
        }

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("project", project);
        model.addAttribute("unreadNotifications", unreadNotifications);
        return "company-project-edit";
    }

    @PostMapping("/company/project/edit/{id}")
    public String updateProject(
            @PathVariable Long id,
            Project formProject,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null || !project.getCompanyEmail().equalsIgnoreCase(company.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Project not found or unauthorized.");
            return "redirect:/company/projects";
        }

        project.setTitle(formProject.getTitle());
        project.setDescription(formProject.getDescription());
        project.setDomain(formProject.getDomain());
        project.setRequiredSkills(formProject.getRequiredSkills());
        project.setDuration(formProject.getDuration());
        project.setDifficultyLevel(formProject.getDifficultyLevel());
        project.setNumberOfStudents(formProject.getNumberOfStudents());
        project.setStartDate(formProject.getStartDate());
        project.setEndDate(formProject.getEndDate());
        project.setStatus(formProject.getStatus());

        projectRepository.save(project);

        // Keep active registrations and reports in sync if project title was updated
        try {
            List<ProjectRegistration> regs = registrationRepository.findAll().stream()
                    .filter(r -> id.equals(r.getProjectId()))
                    .collect(Collectors.toList());
            for (ProjectRegistration r : regs) {
                r.setProjectTitle(project.getTitle());
                registrationRepository.save(r);
            }
            List<ProjectReport> reps = reportRepository.findAll().stream()
                    .filter(r -> id.equals(r.getProjectId()))
                    .collect(Collectors.toList());
            for (ProjectReport rep : reps) {
                rep.setProjectTitle(project.getTitle());
                reportRepository.save(rep);
            }
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("message", "Project updated successfully!");
        return "redirect:/company/projects";
    }

    @PostMapping("/company/project/delete/{id}")
    public String deleteProject(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        Project project = projectRepository.findById(id).orElse(null);
        if (project != null && project.getCompanyEmail().equalsIgnoreCase(company.getEmail())) {
            try {
                List<ProjectReport> reps = reportRepository.findAll().stream()
                        .filter(r -> id.equals(r.getProjectId()))
                        .collect(Collectors.toList());
                reportRepository.deleteAll(reps);

                List<ProjectRegistration> regs = registrationRepository.findAll().stream()
                        .filter(r -> id.equals(r.getProjectId()))
                        .collect(Collectors.toList());
                registrationRepository.deleteAll(regs);
            } catch (Exception ignored) {}

            projectRepository.delete(project);
            redirectAttributes.addFlashAttribute("message", "Project deleted successfully.");
        }

        return "redirect:/company/projects";
    }

    // =====================================================
    // REGISTERED STUDENTS / APPLICANTS
    // =====================================================

    @GetMapping({"/company/registered-students", "/company/registrations"})
    public String registeredStudents(
            @RequestParam(required = false) String email,
            HttpSession session,
            Model model) {

        Company company = getAuthenticatedCompany(session);
        if (company == null && email != null) {
            company = companyRepository.findByEmail(email).orElse(null);
            if (company != null) session.setAttribute("company", company);
        }

        if (company == null) {
            return "redirect:/company/login";
        }

        final String compEmail = company.getEmail();
        List<Project> companyProjects = projectRepository.findByCompanyEmail(compEmail);
        Set<Long> projectIds = companyProjects.stream().map(Project::getId).collect(Collectors.toSet());

        List<ProjectRegistration> registrations = registrationRepository.findAll().stream()
                .filter(r -> projectIds.contains(r.getProjectId()) || (r.getCompanyEmail() != null && r.getCompanyEmail().equalsIgnoreCase(compEmail)))
                .collect(Collectors.toList());

        // Attach student profile info (department, year, skills) to view if needed
        Map<Long, Student> studentMap = new HashMap<>();
        for (ProjectRegistration reg : registrations) {
            if (reg.getStudentId() != null) {
                studentRepository.findById(reg.getStudentId()).ifPresent(s -> studentMap.put(reg.getStudentId(), s));
            }
        }

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("registrations", registrations);
        model.addAttribute("studentMap", studentMap);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-registrations";
    }

    // =====================================================
    // ACCEPT / REJECT
    // =====================================================

    @PostMapping("/company/registration/accept")
    public String acceptStudent(
            @RequestParam Long registrationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg != null) {
            reg.setStatus("ACCEPTED");
            if (reg.getCurrentMilestone() == null || reg.getCurrentMilestone().trim().isEmpty()) {
                reg.setCurrentMilestone("1. Project Planning");
                reg.setMilestoneStatus("IN_PROGRESS");
            }
            registrationRepository.save(reg);

            // Notify student
            notificationService.notifyStudent(reg.getStudentEmail(), "Application Accepted! 🎉",
                    "Congratulations! Your application for '" + reg.getProjectTitle() + "' has been accepted by " + company.getCompanyName() + ". You can now begin project milestones.",
                    "/student/progress?registrationId=" + reg.getId());

            redirectAttributes.addFlashAttribute("message", "Application for " + reg.getStudentName() + " has been ACCEPTED.");
        }

        return "redirect:/company/registered-students";
    }

    @PostMapping("/company/registration/reject")
    public String rejectStudent(
            @RequestParam Long registrationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg != null) {
            reg.setStatus("REJECTED");
            registrationRepository.save(reg);

            // Notify student
            notificationService.notifyStudent(reg.getStudentEmail(), "Application Status Update",
                    "Thank you for your interest. Your application for '" + reg.getProjectTitle() + "' was not selected at this time.",
                    "/student/my-registrations");

            redirectAttributes.addFlashAttribute("message", "Application for " + reg.getStudentName() + " has been marked as REJECTED.");
        }

        return "redirect:/company/registered-students";
    }

    // =====================================================
    // PROGRESS TRACKING
    // =====================================================

    @GetMapping({"/company/progress", "/company/registration/track"})
    public String trackProgress(
            @RequestParam(required = false) Long registrationId,
            HttpSession session,
            Model model) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        final String compEmail = company.getEmail();
        List<Project> companyProjects = projectRepository.findByCompanyEmail(compEmail);
        Set<Long> projectIds = companyProjects.stream().map(Project::getId).collect(Collectors.toSet());

        List<ProjectRegistration> acceptedRegistrations = registrationRepository.findAll().stream()
                .filter(r -> projectIds.contains(r.getProjectId()) || (r.getCompanyEmail() != null && r.getCompanyEmail().equalsIgnoreCase(compEmail)))
                .filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(r.getStatus()) || "COMPLETED".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        ProjectRegistration selectedRegistration = null;
        if (registrationId != null) {
            selectedRegistration = registrationRepository.findById(registrationId).orElse(null);
        }

        if (selectedRegistration == null && !acceptedRegistrations.isEmpty()) {
            selectedRegistration = acceptedRegistrations.get(0);
        }

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("registrations", acceptedRegistrations);
        model.addAttribute("registration", selectedRegistration);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-track-progress";
    }

    // =====================================================
    // REPORTS & EVALUATION
    // =====================================================

    @GetMapping("/company/reports")
    public String reportsList(HttpSession session, Model model) {
        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        List<Project> companyProjects = projectRepository.findByCompanyEmail(company.getEmail());
        Set<Long> projectIds = companyProjects.stream().map(Project::getId).collect(Collectors.toSet());

        List<ProjectReport> reports = reportRepository.findAll().stream()
                .filter(r -> projectIds.contains(r.getProjectId()))
                .collect(Collectors.toList());

        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("reports", reports);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-reports";
    }

    @GetMapping("/company/registration/report")
    public String viewReport(
            @RequestParam Long registrationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        ProjectRegistration registration = registrationRepository.findById(registrationId).orElse(null);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "Registration not found.");
            return "redirect:/company/registered-students";
        }

        ProjectReport report = reportRepository.findByRegistrationId(registrationId).orElse(null);
        long unreadNotifications = notificationService.getUnreadCount(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("registration", registration);
        model.addAttribute("report", report);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "company-view-report";
    }

    @PostMapping("/company/report/feedback")
    public String submitReportFeedback(
            @RequestParam Long reportId,
            @RequestParam String companyFeedback,
            @RequestParam(required = false) String scoreOrGrade,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        ProjectReport report = reportRepository.findById(reportId).orElse(null);
        if (report != null) {
            report.setCompanyFeedback(companyFeedback);
            report.setScoreOrGrade(scoreOrGrade);
            report.setFeedbackDate(LocalDateTime.now().format(DATE_FORMATTER));
            report.setStatus("REVIEWED");
            reportRepository.save(report);

            // Notify student
            notificationService.notifyStudent(report.getStudentEmail(), "Project Report Reviewed",
                    company.getCompanyName() + " has reviewed your final report for '" + report.getProjectTitle() + "'. Feedback: " + companyFeedback,
                    "/student/report/view?registrationId=" + report.getRegistrationId());

            redirectAttributes.addFlashAttribute("message", "Evaluation & feedback submitted successfully!");
            return "redirect:/company/registration/report?registrationId=" + report.getRegistrationId();
        }

        return "redirect:/company/reports";
    }

    @GetMapping("/company/report/download/{id}")
    public void downloadReport(
            @PathVariable Long id,
            HttpSession session,
            HttpServletResponse response) {

        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return;
        }

        ProjectReport report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            return;
        }

        try {
            byte[] pdfBytes = pdfExportService.generateProjectReportPdf(report);
            response.setContentType("application/pdf");
            String filename = "Capstone_Report_" + report.getStudentName().replaceAll("\\s+", "_") + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // NOTIFICATIONS
    // =====================================================

    @GetMapping("/company/notifications")
    public String notifications(HttpSession session, Model model) {
        Company company = getAuthenticatedCompany(session);
        if (company == null) {
            return "redirect:/company/login";
        }

        List<Notification> notificationList = notificationService.getNotificationsForUser(company.getEmail());
        notificationService.markAllAsRead(company.getEmail());

        model.addAttribute("company", company);
        model.addAttribute("notifications", notificationList);

        return "company-notifications";
    }

    // =====================================================
    // LOGOUT
    // =====================================================

    @GetMapping("/company/logout")
    public String logout(HttpSession session) {
        Company company = getAuthenticatedCompany(session);
        if (company != null) {
            auditLogService.log(company.getEmail(), "COMPANY", "LOGOUT", "Company logged out: " + company.getCompanyName());
        }
        session.invalidate();
        return "redirect:/company/login";
    }
}