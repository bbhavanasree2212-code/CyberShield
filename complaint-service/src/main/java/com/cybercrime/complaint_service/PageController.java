
package com.cybercrime.complaint_service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Open login page when visiting the root URL
    @GetMapping("/")
    public String loginPage() {
        return "forward:/login.html";
    }

    // Dashboard page
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "forward:/dashboard.html";
    }

    // Help and Safety page
    @GetMapping("/help-safety")
    public String helpSafetyPage() {
        return "forward:/help-safety.html";
    }

    // Historical complaints page
    @GetMapping("/historical")
    public String historicalPage() {
        return "forward:/historical.html";
    }

    // Alerts page
    @GetMapping("/alerts")
    public String alertsPage() {
        return "forward:/alerts.html";
    }

    // Investigations page
    @GetMapping("/investigations")
    public String investigationsPage() {
        return "forward:/investigations.html";
    }

    // Evidence page
    @GetMapping("/evidence")
    public String evidencePage() {
        return "forward:/evidence.html";
    }

    // Reports page
    @GetMapping("/reports")
    public String reportsPage() {
        return "forward:/reports.html";
    }

    // Complaints page
    @GetMapping("/complaints")
    public String complaintsPage() {
        return "forward:/complaints.html";
    }

    // AI analysis page
    @GetMapping("/ai-analysis")
    public String aiAnalysisPage() {
        return "forward:/ai-analysis.html";
    }
}
