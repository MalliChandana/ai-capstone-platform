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
public class StudentController {

    private final StudentRepository studentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectRegistrationRepository registrationRepository;
    private final ProjectReportRepository reportRepository;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;
    private final PdfExportService pdfExportService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StudentController(
            StudentRepository studentRepository,
            ProjectRepository projectRepository,
            ProjectRegistrationRepository registrationRepository,
            ProjectReportRepository reportRepository,
            RecommendationService recommendationService,
            NotificationService notificationService,
            PdfExportService pdfExportService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {

        this.studentRepository = studentRepository;
        this.projectRepository = projectRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.recommendationService = recommendationService;
        this.notificationService = notificationService;
        this.pdfExportService = pdfExportService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    // Helper to get logged-in student or null
    private Student getAuthenticatedStudent(HttpSession session) {
        Object obj = session.getAttribute("student");
        if (obj instanceof Student) {
            return (Student) obj;
        }
        return null;
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @GetMapping("/student/login")
    public String loginPage(HttpSession session) {
        if (getAuthenticatedStudent(session) != null) {
            return "redirect:/student/dashboard";
        }
        return "student-login";
    }

    @PostMapping("/student/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Student> opt = studentRepository.findByEmail(email.trim());
        if (opt.isEmpty()) {
            auditLogService.log(email.trim(), "STUDENT", "LOGIN_FAILED", "Login failed: student email not found");
            model.addAttribute("error", "No student account found with this email.");
            return "student-login";
        }

        Student student = opt.get();
        if (Boolean.FALSE.equals(student.getActive())) {
            auditLogService.log(email.trim(), "STUDENT", "LOGIN_BLOCKED", "Login blocked: student account deactivated by administrator");
            model.addAttribute("error", "Your student account has been deactivated. Please contact the administrator.");
            return "student-login";
        }

        boolean passwordMatch = false;
        if (passwordEncoder.matches(password, student.getPassword())) {
            passwordMatch = true;
        } else if (student.getPassword().equals(password)) {
            // Upgrade plaintext password to BCrypt hash transparently
            student.setPassword(passwordEncoder.encode(password));
            studentRepository.save(student);
            passwordMatch = true;
        }

        if (!passwordMatch) {
            auditLogService.log(email.trim(), "STUDENT", "LOGIN_FAILED", "Login failed: incorrect password");
            model.addAttribute("error", "Incorrect password. Please try again.");
            return "student-login";
        }

        session.setAttribute("student", student);
        auditLogService.log(student.getEmail(), "STUDENT", "LOGIN", "Student logged in successfully");
        return "redirect:/student/dashboard";
    }

    // =====================================================
    // REGISTRATION
    // =====================================================

    @GetMapping("/student/register")
    public String registerPage(HttpSession session) {
        if (getAuthenticatedStudent(session) != null) {
            return "redirect:/student/dashboard";
        }
        return "student-register";
    }

    @PostMapping("/student/register")
    public String register(
            Student student,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (student.getEmail() == null || student.getEmail().trim().isEmpty() ||
            student.getPassword() == null || student.getPassword().trim().isEmpty() ||
            student.getName() == null || student.getName().trim().isEmpty()) {
            model.addAttribute("error", "Please fill in all required fields.");
            return "student-register";
        }

        if (studentRepository.findByEmail(student.getEmail().trim()).isPresent()) {
            model.addAttribute("error", "An account with email '" + student.getEmail() + "' already exists. Please log in.");
            return "student-register";
        }

        student.setEmail(student.getEmail().trim());
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setActive(true);
        studentRepository.save(student);

        auditLogService.log(student.getEmail(), "STUDENT", "REGISTER", "New student account registered: " + student.getName());

        // Send welcome notification
        notificationService.notifyStudent(student.getEmail(), "Welcome to AI Capstone Platform",
                "Your student account has been created. Start by exploring AI project recommendations!", "/student/recommendations");

        redirectAttributes.addFlashAttribute("message", "Account registered successfully! Please log in.");
        return "redirect:/student/login";
    }

    private List<Project> getAvailableUniqueProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> p.getTitle() != null && !p.getTitle().trim().isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                p -> (p.getTitle().trim().toLowerCase() + ":::" + (p.getCompanyEmail() != null ? p.getCompanyEmail().trim().toLowerCase() : "")),
                                p -> p,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    @GetMapping("/student/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        // Refresh student from DB
        student = studentRepository.findById(student.getId()).orElse(student);
        session.setAttribute("student", student);

        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        List<Project> allProjects = getAvailableUniqueProjects();

        long registeredCount = registrations.size();
        long activeCount = registrations.stream()
                .filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(r.getStatus()))
                .count();
        long completedCount = registrations.stream()
                .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                .count();

        // Calculate average match score across projects
        List<RecommendationService.ProjectMatchResult> recommendations = recommendationService.getRecommendations(student, allProjects);
        int topMatchScore = recommendations.isEmpty() ? 0 : recommendations.get(0).getMatchPercentage();

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registeredCount", registeredCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("topMatchScore", topMatchScore);
        model.addAttribute("topRecommendations", recommendations.stream().limit(3).collect(Collectors.toList()));
        model.addAttribute("recentRegistrations", registrations.stream().limit(4).collect(Collectors.toList()));
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-dashboard";
    }

    // =====================================================
    // RECOMMENDATIONS
    // =====================================================

    @GetMapping("/student/recommendations")
    public String recommendations(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        student = studentRepository.findById(student.getId()).orElse(student);

        List<Project> allProjects = getAvailableUniqueProjects();
        List<RecommendationService.ProjectMatchResult> recommendations = recommendationService.getRecommendations(student, allProjects);

        // Fetch user's registered project IDs to show status
        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        Set<Long> registeredProjectIds = registrations.stream().map(ProjectRegistration::getProjectId).collect(Collectors.toSet());

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());
        model.addAttribute("student", student);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("registeredProjectIds", registeredProjectIds);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-recommendations";
    }

    // =====================================================
    // SKILL GAP ANALYSIS
    // =====================================================

    @GetMapping("/student/skill-gap")
    public String skillGap(
            @RequestParam(required = false) Long projectId,
            HttpSession session,
            Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        student = studentRepository.findById(student.getId()).orElse(student);

        List<Project> allProjects = getAvailableUniqueProjects();
        Project selectedProject = null;

        if (projectId != null) {
            selectedProject = projectRepository.findById(projectId).orElse(null);
        }

        if (selectedProject == null && !allProjects.isEmpty()) {
            // Default to top recommended project
            List<RecommendationService.ProjectMatchResult> recs = recommendationService.getRecommendations(student, allProjects);
            if (!recs.isEmpty()) {
                selectedProject = recs.get(0).getProject();
            } else {
                selectedProject = allProjects.get(0);
            }
        }

        RecommendationService.ProjectMatchResult matchResult = null;
        if (selectedProject != null) {
            matchResult = recommendationService.analyzeSkillGap(student, selectedProject);
        }

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("allProjects", allProjects);
        model.addAttribute("selectedProject", selectedProject);
        model.addAttribute("matchResult", matchResult);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-skill-gap";
    }

    // =====================================================
    // MY SKILLS
    // =====================================================

    @GetMapping("/student/skills")
    public String skills(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        student = studentRepository.findById(student.getId()).orElse(student);
        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());
        model.addAttribute("student", student);
        model.addAttribute("unreadNotifications", unreadNotifications);

        // Parse skills into a structured list
        List<String> skillList = new ArrayList<>();
        if (student.getSkills() != null && !student.getSkills().trim().isEmpty()) {
            String[] parts = student.getSkills().split("[,;\\n]+");
            for (String p : parts) {
                String s = p.trim();
                if (!s.isEmpty()) skillList.add(s);
            }
        }
        model.addAttribute("skillList", skillList);

        return "student-skills";
    }

    @PostMapping("/student/skills/update")
    public String updateSkills(
            @RequestParam String skills,
            @RequestParam(required = false) String interests,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        student = studentRepository.findById(student.getId()).orElse(student);
        student.setSkills(skills);
        if (interests != null) {
            student.setInterests(interests);
        }

        studentRepository.save(student);
        session.setAttribute("student", student);

        redirectAttributes.addFlashAttribute("message", "Your technical skills & profile have been updated successfully!");
        return "redirect:/student/skills";
    }

    // =====================================================
    // EXPLORE PROJECTS (With Search, Filter, Sort)
    // =====================================================

    @GetMapping("/student/projects")
    public String projects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            HttpSession session,
            Model model) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        student = studentRepository.findById(student.getId()).orElse(student);

        List<Project> allProjects = getAvailableUniqueProjects();

        // Apply filters
        List<Project> filtered = allProjects.stream().filter(p -> {
            boolean match = true;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.toLowerCase().trim();
                boolean inTitle = p.getTitle() != null && p.getTitle().toLowerCase().contains(kw);
                boolean inDesc = p.getDescription() != null && p.getDescription().toLowerCase().contains(kw);
                boolean inSkills = p.getRequiredSkills() != null && p.getRequiredSkills().toLowerCase().contains(kw);
                boolean inCompany = p.getCompanyName() != null && p.getCompanyName().toLowerCase().contains(kw);
                match = inTitle || inDesc || inSkills || inCompany;
            }
            if (match && domain != null && !domain.trim().isEmpty() && !"ALL".equalsIgnoreCase(domain)) {
                match = p.getDomain() != null && p.getDomain().equalsIgnoreCase(domain.trim());
            }
            if (match && skill != null && !skill.trim().isEmpty()) {
                match = p.getRequiredSkills() != null && p.getRequiredSkills().toLowerCase().contains(skill.toLowerCase().trim());
            }
            if (match && status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                match = p.getStatus() != null && p.getStatus().equalsIgnoreCase(status.trim());
            }
            return match;
        }).collect(Collectors.toList());

        // Match results for filtered projects
        List<RecommendationService.ProjectMatchResult> projectMatches = new ArrayList<>();
        for (Project p : filtered) {
            projectMatches.add(recommendationService.analyzeSkillGap(student, p));
        }

        // Sorting
        if ("matchDesc".equalsIgnoreCase(sortBy) || sortBy == null || sortBy.isEmpty()) {
            projectMatches.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));
        } else if ("titleAsc".equalsIgnoreCase(sortBy)) {
            projectMatches.sort(Comparator.comparing(a -> a.getProject().getTitle() != null ? a.getProject().getTitle() : ""));
        } else if ("startDate".equalsIgnoreCase(sortBy)) {
            projectMatches.sort(Comparator.comparing(a -> a.getProject().getStartDate() != null ? a.getProject().getStartDate() : ""));
        }

        // Fetch distinct domains for filter dropdown
        Set<String> distinctDomains = allProjects.stream()
                .map(Project::getDomain)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        // Registered project IDs
        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        Set<Long> registeredProjectIds = registrations.stream().map(ProjectRegistration::getProjectId).collect(Collectors.toSet());

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("projectMatches", projectMatches);
        model.addAttribute("distinctDomains", distinctDomains);
        model.addAttribute("registeredProjectIds", registeredProjectIds);
        model.addAttribute("keyword", keyword);
        model.addAttribute("domain", domain);
        model.addAttribute("skill", skill);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-projects";
    }

    // =====================================================
    // PROJECT DETAILS PAGE
    // =====================================================

    @GetMapping("/student/project/{id}")
    public String projectDetails(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return "redirect:/student/projects";
        }

        RecommendationService.ProjectMatchResult matchResult = recommendationService.analyzeSkillGap(student, project);
        boolean isRegistered = registrationRepository.existsByStudentIdAndProjectId(student.getId(), project.getId());
        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("project", project);
        model.addAttribute("matchResult", matchResult);
        model.addAttribute("isRegistered", isRegistered);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-project-details";
    }

    // =====================================================
    // REGISTER FOR PROJECT
    // =====================================================

    @PostMapping("/student/project/register")
    public String registerProject(
            @RequestParam Long projectId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            redirectAttributes.addFlashAttribute("error", "Selected project was not found.");
            return "redirect:/student/projects";
        }

        boolean alreadyRegistered = registrationRepository.existsByStudentIdAndProjectId(student.getId(), project.getId());
        if (alreadyRegistered) {
            redirectAttributes.addFlashAttribute("error", "You have already registered for this capstone project.");
            return "redirect:/student/my-registrations";
        }

        ProjectRegistration reg = new ProjectRegistration();
        reg.setStudentId(student.getId());
        reg.setProjectId(project.getId());
        reg.setStudentName(student.getName());
        reg.setStudentEmail(student.getEmail());
        reg.setProjectTitle(project.getTitle());
        reg.setCompanyEmail(project.getCompanyEmail());
        reg.setRegistrationDate(LocalDateTime.now().format(DATE_FORMATTER));
        reg.setStatus("PENDING");
        reg.setProgress(0);
        reg.setCurrentMilestone("1. Project Planning");
        reg.setMilestoneStatus("NOT_STARTED");
        reg.setStudentRemarks("Initial application submitted.");

        registrationRepository.save(reg);

        // Notify company
        if (project.getCompanyEmail() != null) {
            notificationService.notifyCompany(project.getCompanyEmail(), "New Student Application",
                    student.getName() + " has applied for '" + project.getTitle() + "'.", "/company/registered-students");
        }

        // Notify student
        notificationService.notifyStudent(student.getEmail(), "Application Submitted",
                "Your application for '" + project.getTitle() + "' has been submitted and is pending review.", "/student/my-registrations");

        redirectAttributes.addFlashAttribute("message", "Application submitted successfully! Track your status under My Projects.");
        return "redirect:/student/my-registrations";
    }

    // =====================================================
    // MY REGISTRATIONS / MY PROJECTS
    // =====================================================

    @GetMapping("/student/my-registrations")
    public String myRegistrations(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());

        // Check if report submitted for each registration
        Map<Long, Boolean> reportSubmittedMap = new HashMap<>();
        for (ProjectRegistration reg : registrations) {
            reportSubmittedMap.put(reg.getId(), reportRepository.findByRegistrationId(reg.getId()).isPresent());
        }

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registrations", registrations);
        model.addAttribute("reportSubmittedMap", reportSubmittedMap);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-registrations";
    }

    // =====================================================
    // PROGRESS TRACKING & MILESTONES
    // =====================================================

    @GetMapping("/student/progress")
    public String progressPage(
            @RequestParam(required = false) Long registrationId,
            HttpSession session,
            Model model) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        List<ProjectRegistration> activeRegistrations = registrations.stream()
                .filter(r -> "ACCEPTED".equalsIgnoreCase(r.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(r.getStatus()) || "COMPLETED".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        ProjectRegistration selectedRegistration = null;
        if (registrationId != null) {
            selectedRegistration = activeRegistrations.stream()
                    .filter(r -> r.getId().equals(registrationId))
                    .findFirst()
                    .orElse(null);
        }

        if (selectedRegistration == null && !activeRegistrations.isEmpty()) {
            selectedRegistration = activeRegistrations.get(0);
        }

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registrations", activeRegistrations);
        model.addAttribute("selectedRegistration", selectedRegistration);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-progress";
    }

    @PostMapping("/student/progress/update")
    public String updateProgress(
            @RequestParam Long registrationId,
            @RequestParam int progress,
            @RequestParam String currentMilestone,
            @RequestParam String milestoneStatus,
            @RequestParam String studentRemarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || !reg.getStudentId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Registration not found or unauthorized.");
            return "redirect:/student/progress";
        }

        int clampedProgress = Math.max(0, Math.min(100, progress));
        reg.setProgress(clampedProgress);
        reg.setCurrentMilestone(currentMilestone);
        reg.setMilestoneStatus(milestoneStatus);
        reg.setStudentRemarks(studentRemarks);

        if (clampedProgress == 100 || "Completed".equalsIgnoreCase(milestoneStatus) || "COMPLETED".equalsIgnoreCase(milestoneStatus)) {
            reg.setStatus("COMPLETED");
            reg.setMilestoneStatus("COMPLETED");
        } else {
            reg.setStatus("IN_PROGRESS");
        }

        registrationRepository.save(reg);

        // Notify company
        if (reg.getCompanyEmail() != null) {
            notificationService.notifyCompany(reg.getCompanyEmail(), "Student Progress Update",
                    student.getName() + " updated progress to " + clampedProgress + "% (" + currentMilestone + ") for '" + reg.getProjectTitle() + "'.",
                    "/company/registration/track?registrationId=" + reg.getId());
        }

        redirectAttributes.addFlashAttribute("message", "Progress updated successfully!");
        return "redirect:/student/progress?registrationId=" + reg.getId();
    }

    // =====================================================
    // REPORT SUBMISSION & VIEW
    // =====================================================

    @GetMapping("/student/report/submit")
    public String submitReportPage(
            @RequestParam Long registrationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || !reg.getStudentId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Registration not found.");
            return "redirect:/student/my-registrations";
        }

        ProjectReport report = reportRepository.findByRegistrationId(registrationId).orElse(new ProjectReport());
        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registration", reg);
        model.addAttribute("report", report);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-report-submit";
    }

    @PostMapping("/student/report/submit")
    public String submitReport(
            @RequestParam Long registrationId,
            ProjectReport formReport,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || !reg.getStudentId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized report submission.");
            return "redirect:/student/my-registrations";
        }

        ProjectReport report = reportRepository.findByRegistrationId(registrationId).orElse(new ProjectReport());
        report.setRegistrationId(reg.getId());
        report.setProjectId(reg.getProjectId());
        report.setStudentId(student.getId());
        report.setStudentName(student.getName());
        report.setStudentEmail(student.getEmail());
        report.setProjectTitle(reg.getProjectTitle());

        report.setReportTitle(formReport.getReportTitle());
        report.setAbstractText(formReport.getAbstractText());
        report.setObjectives(formReport.getObjectives());
        report.setTechnologiesUsed(formReport.getTechnologiesUsed());
        report.setMethodology(formReport.getMethodology());
        report.setResults(formReport.getResults());
        report.setConclusion(formReport.getConclusion());
        report.setFutureScope(formReport.getFutureScope());
        report.setSubmittedDate(LocalDateTime.now().format(DATE_FORMATTER));
        report.setStatus("SUBMITTED");

        reportRepository.save(report);

        // Notify company
        if (reg.getCompanyEmail() != null) {
            notificationService.notifyCompany(reg.getCompanyEmail(), "Final Project Report Submitted",
                    student.getName() + " has submitted their project report for '" + reg.getProjectTitle() + "'.",
                    "/company/registration/report?registrationId=" + reg.getId());
        }

        // Notify student
        notificationService.notifyStudent(student.getEmail(), "Report Submitted",
                "Your project report for '" + reg.getProjectTitle() + "' has been submitted for company review.",
                "/student/report/view?registrationId=" + reg.getId());

        redirectAttributes.addFlashAttribute("message", "Project report submitted successfully! Awaiting company review.");
        return "redirect:/student/report/view?registrationId=" + reg.getId();
    }

    @GetMapping("/student/report/view")
    public String viewReport(
            @RequestParam Long registrationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        ProjectRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || !reg.getStudentId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Registration not found.");
            return "redirect:/student/my-registrations";
        }

        ProjectReport report = reportRepository.findByRegistrationId(registrationId).orElse(null);
        if (report == null) {
            return "redirect:/student/report/submit?registrationId=" + registrationId;
        }

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registration", reg);
        model.addAttribute("report", report);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-report-view";
    }

    @GetMapping("/student/report/download/{id}")
    public void downloadReport(
            @PathVariable Long id,
            HttpSession session,
            HttpServletResponse response) {

        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return;
        }

        ProjectReport report = reportRepository.findById(id).orElse(null);
        if (report == null || !report.getStudentId().equals(student.getId())) {
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
    // ALL STUDENT REPORTS
    // =====================================================

    @GetMapping("/student/reports")
    public String studentReports(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        List<ProjectRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        Map<Long, ProjectReport> reportMap = new HashMap<>();
        for (ProjectRegistration reg : registrations) {
            reportRepository.findByRegistrationId(reg.getId()).ifPresent(rep -> reportMap.put(reg.getId(), rep));
        }

        long unreadNotifications = notificationService.getUnreadCount(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("registrations", registrations);
        model.addAttribute("reportMap", reportMap);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student-reports";
    }

    // =====================================================
    // NOTIFICATIONS
    // =====================================================

    @GetMapping("/student/notifications")
    public String notifications(HttpSession session, Model model) {
        Student student = getAuthenticatedStudent(session);
        if (student == null) {
            return "redirect:/student/login";
        }

        List<Notification> notificationList = notificationService.getNotificationsForUser(student.getEmail());
        notificationService.markAllAsRead(student.getEmail());

        model.addAttribute("student", student);
        model.addAttribute("notifications", notificationList);
        model.addAttribute("unreadNotifications", 0L);

        return "student-notifications";
    }

    // =====================================================
    // LOGOUT
    // =====================================================

    @GetMapping("/student/logout")
    public String logout(HttpSession session) {
        Student student = getAuthenticatedStudent(session);
        if (student != null) {
            auditLogService.log(student.getEmail(), "STUDENT", "LOGOUT", "Student logged out");
        }
        session.invalidate();
        return "redirect:/student/login";
    }
}