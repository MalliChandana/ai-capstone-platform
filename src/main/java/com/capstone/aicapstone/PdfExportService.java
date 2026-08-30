package com.capstone.aicapstone;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class PdfExportService {

    public byte[] generateProjectReportPdf(ProjectReport report) throws DocumentException {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(23, 37, 84));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(37, 99, 235));
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(23, 37, 84));
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(55, 65, 81));

        // Header Title
        Paragraph title = new Paragraph("CAPSTONE PROJECT FINAL REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph sub = new Paragraph("AI Capstone Project Recommendation & Management Platform", subtitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(18);
        document.add(sub);

        // Project & Student Metadata Table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(15);

        addCell(table, "Project Title:", report.getProjectTitle(), boldFont, normalFont);
        addCell(table, "Report Title:", report.getReportTitle() != null ? report.getReportTitle() : "Final Submission", boldFont, normalFont);
        addCell(table, "Student Name:", report.getStudentName(), boldFont, normalFont);
        addCell(table, "Student Email:", report.getStudentEmail(), boldFont, normalFont);
        addCell(table, "Submission Date:", report.getSubmittedDate() != null ? report.getSubmittedDate() : "N/A", boldFont, normalFont);
        addCell(table, "Evaluation Status:", report.getStatus() != null ? report.getStatus() : "SUBMITTED", boldFont, normalFont);

        document.add(table);

        // Sections
        addSection(document, "1. Abstract", report.getAbstractText(), sectionHeaderFont, normalFont);
        addSection(document, "2. Project Objectives", report.getObjectives(), sectionHeaderFont, normalFont);
        addSection(document, "3. Technologies & Tools Used", report.getTechnologiesUsed(), sectionHeaderFont, normalFont);
        addSection(document, "4. Methodology & Implementation", report.getMethodology(), sectionHeaderFont, normalFont);
        addSection(document, "5. Results & Discussion", report.getResults(), sectionHeaderFont, normalFont);
        addSection(document, "6. Conclusion", report.getConclusion(), sectionHeaderFont, normalFont);
        addSection(document, "7. Future Scope", report.getFutureScope(), sectionHeaderFont, normalFont);

        // Company Feedback if available
        if (report.getCompanyFeedback() != null && !report.getCompanyFeedback().trim().isEmpty()) {
            String feedbackContent = report.getCompanyFeedback()
                    + (report.getScoreOrGrade() != null ? "\nScore/Grade: " + report.getScoreOrGrade() : "")
                    + (report.getFeedbackDate() != null ? "\nReviewed On: " + report.getFeedbackDate() : "");
            addSection(document, "8. Company Review & Evaluation Feedback", feedbackContent, sectionHeaderFont, normalFont);
        }

        document.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBackgroundColor(new Color(245, 247, 251));
        c1.setPadding(6);
        c1.setBorderColor(new Color(229, 231, 235));

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", valFont));
        c2.setPadding(6);
        c2.setBorderColor(new Color(229, 231, 235));

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addSection(Document doc, String title, String content, Font headerFont, Font bodyFont) throws DocumentException {
        Paragraph h = new Paragraph(title, headerFont);
        h.setSpacingBefore(10);
        h.setSpacingAfter(4);
        doc.add(h);

        Paragraph p = new Paragraph(content != null && !content.trim().isEmpty() ? content : "No content provided.", bodyFont);
        p.setSpacingAfter(10);
        p.setLeading(14);
        doc.add(p);
    }
}
