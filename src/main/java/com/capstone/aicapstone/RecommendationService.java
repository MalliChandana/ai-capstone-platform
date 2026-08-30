package com.capstone.aicapstone;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    public static class ProjectMatchResult {
        private Project project;
        private int matchPercentage;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private String recommendationReason;
        private List<String> learningRecommendations;

        public ProjectMatchResult() {
        }

        public ProjectMatchResult(Project project, int matchPercentage, List<String> matchedSkills,
                                  List<String> missingSkills, String recommendationReason,
                                  List<String> learningRecommendations) {
            this.project = project;
            this.matchPercentage = matchPercentage;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
            this.recommendationReason = recommendationReason;
            this.learningRecommendations = learningRecommendations;
        }

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public int getMatchPercentage() {
            return matchPercentage;
        }

        public void setMatchPercentage(int matchPercentage) {
            this.matchPercentage = matchPercentage;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public void setMatchedSkills(List<String> matchedSkills) {
            this.matchedSkills = matchedSkills;
        }

        public List<String> getMissingSkills() {
            return missingSkills;
        }

        public void setMissingSkills(List<String> missingSkills) {
            this.missingSkills = missingSkills;
        }

        public String getRecommendationReason() {
            return recommendationReason;
        }

        public void setRecommendationReason(String recommendationReason) {
            this.recommendationReason = recommendationReason;
        }

        public List<String> getLearningRecommendations() {
            return learningRecommendations;
        }

        public void setLearningRecommendations(List<String> learningRecommendations) {
            this.learningRecommendations = learningRecommendations;
        }
    }

    private static final Map<String, List<String>> LEARNING_TOPIC_MAP = new HashMap<>();

    static {
        LEARNING_TOPIC_MAP.put("python", Arrays.asList("Python Advanced Concepts & OOP", "Data Structures in Python", "Package Management & Virtual Environments"));
        LEARNING_TOPIC_MAP.put("java", Arrays.asList("Spring Boot & RESTful APIs", "Java Multithreading & Concurrency", "Hibernate & JPA Performance"));
        LEARNING_TOPIC_MAP.put("machine learning", Arrays.asList("Supervised & Unsupervised Learning Algorithms", "Feature Engineering & Preprocessing", "Model Evaluation Metrics (ROC/AUC, F1-Score)"));
        LEARNING_TOPIC_MAP.put("deep learning", Arrays.asList("Artificial Neural Networks (ANN)", "Convolutional Neural Networks (CNN) for Computer Vision", "PyTorch / TensorFlow Model Optimization"));
        LEARNING_TOPIC_MAP.put("tensorflow", Arrays.asList("Keras API Fundamentals", "Transfer Learning Techniques", "TensorFlow Lite & Model Serving"));
        LEARNING_TOPIC_MAP.put("pytorch", Arrays.asList("PyTorch Tensor Operations", "Building Custom Layers & Loss Functions", "Model Training on GPUs"));
        LEARNING_TOPIC_MAP.put("sql", Arrays.asList("Database Indexing & Query Optimization", "Complex Joins & Aggregations", "Schema Normalization & ACID Transactions"));
        LEARNING_TOPIC_MAP.put("mysql", Arrays.asList("Relational DB Design", "Stored Procedures & Triggers", "MySQL Workbench Performance Tuning"));
        LEARNING_TOPIC_MAP.put("react", Arrays.asList("React Hooks (useEffect, useMemo)", "State Management (Redux/Zustand)", "Responsive UI Design with Tailwind CSS"));
        LEARNING_TOPIC_MAP.put("javascript", Arrays.asList("ES6+ Syntax & Async/Await", "DOM Manipulation & Events", "Modern JS Module Bundlers"));
        LEARNING_TOPIC_MAP.put("html", Arrays.asList("Semantic HTML5 Markup", "Web Accessibility (a11y) Standards", "SEO Best Practices"));
        LEARNING_TOPIC_MAP.put("css", Arrays.asList("CSS Grid & Flexbox Layouts", "Responsive Design & Media Queries", "Modern CSS Animations"));
        LEARNING_TOPIC_MAP.put("cloud", Arrays.asList("AWS / GCP Cloud Architecture", "Serverless Computing & Microservices", "Cloud Security & IAM"));
        LEARNING_TOPIC_MAP.put("docker", Arrays.asList("Containerization Principles", "Dockerfile Best Practices", "Docker Compose Multi-Container Setup"));
        LEARNING_TOPIC_MAP.put("data analysis", Arrays.asList("Exploratory Data Analysis (EDA) with Pandas", "Data Visualization using Seaborn & Matplotlib", "Handling Missing Values & Outliers"));
        LEARNING_TOPIC_MAP.put("nlp", Arrays.asList("Natural Language Processing Fundamentals", "Tokenization, Stemming & Lemmatization", "Transformers & Hugging Face Libraries"));
        LEARNING_TOPIC_MAP.put("ai", Arrays.asList("AI Algorithm Foundations", "Prompt Engineering & LLM Integration", "Ethical AI & Explainability"));
    }

    public List<ProjectMatchResult> getRecommendations(Student student, List<Project> projects) {
        if (student == null || projects == null || projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Project> uniqueProjects = projects.stream()
                .filter(p -> p != null && p.getTitle() != null && !p.getTitle().trim().isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                p -> (p.getTitle().trim().toLowerCase() + ":::" + (p.getCompanyEmail() != null ? p.getCompanyEmail().trim().toLowerCase() : "")),
                                p -> p,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));

        List<ProjectMatchResult> results = new ArrayList<>();
        for (Project project : uniqueProjects) {
            results.add(analyzeSkillGap(student, project));
        }

        // Sort descending by match percentage
        results.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));
        return results;
    }

    public ProjectMatchResult analyzeSkillGap(Student student, Project project) {
        if (student == null || project == null) {
            return new ProjectMatchResult(project, 50, Collections.emptyList(), Collections.emptyList(),
                    "No student data available", Collections.emptyList());
        }

        Set<String> studentSkillTokens = parseTokens(student.getSkills());
        Set<String> studentInterestTokens = parseTokens(student.getInterests());
        Set<String> studentDeptTokens = parseTokens(student.getDepartment());

        Set<String> projectReqTokens = parseTokens(project.getRequiredSkills());
        Set<String> projectDescTokens = parseTokens(project.getDescription());
        Set<String> projectDomainTokens = parseTokens(project.getDomain());
        Set<String> projectTitleTokens = parseTokens(project.getTitle());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        // If project specifies required skills
        if (projectReqTokens.isEmpty()) {
            // derive from description or title
            projectReqTokens.addAll(extractKeywords(project.getTitle() + " " + project.getDescription()));
        }

        for (String reqSkill : projectReqTokens) {
            boolean matched = false;
            for (String studentSkill : studentSkillTokens) {
                if (isSkillMatch(reqSkill, studentSkill)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                matchedSkills.add(capitalize(reqSkill));
            } else {
                missingSkills.add(capitalize(reqSkill));
            }
        }

        // Score calculation:
        double skillScore = 0.0;
        if (!projectReqTokens.isEmpty()) {
            skillScore = ((double) matchedSkills.size() / (double) (matchedSkills.size() + missingSkills.size())) * 60.0;
        } else {
            skillScore = 40.0;
        }

        // Interest overlap
        double interestScore = 0.0;
        long interestOverlap = studentInterestTokens.stream()
                .filter(t -> projectDescTokens.contains(t) || projectTitleTokens.contains(t) || projectDomainTokens.contains(t))
                .count();
        if (!studentInterestTokens.isEmpty()) {
            interestScore = Math.min(25.0, (interestOverlap * 10.0));
        } else {
            interestScore = 15.0;
        }

        // Department / Domain overlap
        double deptScore = 10.0;
        boolean deptMatch = studentDeptTokens.stream()
                .anyMatch(t -> projectDomainTokens.contains(t) || projectDescTokens.contains(t) || projectTitleTokens.contains(t));
        if (deptMatch) {
            deptScore = 15.0;
        }

        int totalMatch = (int) Math.round(skillScore + interestScore + deptScore);
        if (totalMatch > 98) totalMatch = 98;
        if (totalMatch < 25) totalMatch = 25;

        // Generate tailored recommendation reason
        StringBuilder reason = new StringBuilder();
        if (!matchedSkills.isEmpty()) {
            reason.append("Recommended because your ")
                    .append(String.join(", ", matchedSkills))
                    .append(" skill").append(matchedSkills.size() > 1 ? "s" : "")
                    .append(" strongly match").append(matchedSkills.size() == 1 ? "es" : "")
                    .append(" this capstone project's requirements.");
        } else if (interestOverlap > 0) {
            reason.append("Recommended based on your strong interest in ")
                    .append(project.getDomain() != null ? project.getDomain() : "this project domain")
                    .append(" and academic profile.");
        } else {
            reason.append("Recommended as a high-growth project matching your department curriculum.");
        }

        // Generate learning recommendations
        List<String> learningTopics = new ArrayList<>();
        for (String missing : missingSkills) {
            String key = missing.toLowerCase();
            if (LEARNING_TOPIC_MAP.containsKey(key)) {
                learningTopics.addAll(LEARNING_TOPIC_MAP.get(key));
            } else {
                learningTopics.add(missing + " Fundamentals & Best Practices");
                learningTopics.add("Hands-on " + missing + " Project Tutorial");
            }
        }
        if (learningTopics.isEmpty()) {
            learningTopics.add("System Architecture & Scalability");
            learningTopics.add("Industry Best Practices & Testing");
            learningTopics.add("Production Deployment & CI/CD Pipelines");
        }

        // Deduplicate
        List<String> distinctLearning = learningTopics.stream().distinct().limit(6).collect(Collectors.toList());

        return new ProjectMatchResult(project, totalMatch, matchedSkills, missingSkills, reason.toString(), distinctLearning);
    }

    private Set<String> parseTokens(String text) {
        Set<String> set = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return set;
        }
        String[] parts = text.toLowerCase().split("[,;|/\\n]+");
        for (String p : parts) {
            String trimmed = p.trim().replaceAll("[^a-zA-Z0-9#+.-]", " ").trim();
            if (!trimmed.isEmpty()) {
                set.add(normalize(trimmed));
            }
        }
        return set;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null) return keywords;
        String lower = text.toLowerCase();
        for (String known : LEARNING_TOPIC_MAP.keySet()) {
            if (lower.contains(known)) {
                keywords.add(known);
            }
        }
        return keywords;
    }

    private String normalize(String skill) {
        String s = skill.toLowerCase().trim();
        if (s.equals("ml")) return "machine learning";
        if (s.equals("ai")) return "ai";
        if (s.equals("dl")) return "deep learning";
        if (s.equals("js")) return "javascript";
        if (s.equals("reactjs")) return "react";
        if (s.equals("node") || s.equals("nodejs")) return "node.js";
        if (s.equals("nlp")) return "nlp";
        if (s.equals("db") || s.equals("database")) return "sql";
        return s;
    }

    private boolean isSkillMatch(String skill1, String skill2) {
        String s1 = normalize(skill1);
        String s2 = normalize(skill2);
        if (s1.equals(s2)) return true;
        if (s1.contains(s2) || s2.contains(s1)) return true;
        return false;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.equalsIgnoreCase("ai") || w.equalsIgnoreCase("ml") || w.equalsIgnoreCase("sql") || w.equalsIgnoreCase("nlp") || w.equalsIgnoreCase("api") || w.equalsIgnoreCase("html") || w.equalsIgnoreCase("css")) {
                sb.append(w.toUpperCase()).append(" ");
            } else if (w.length() > 1) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
            } else {
                sb.append(w.toUpperCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
