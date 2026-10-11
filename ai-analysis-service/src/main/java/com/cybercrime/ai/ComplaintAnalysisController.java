package com.cybercrime.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class ComplaintAnalysisController {

    private final List<Map<String, Object>> historicalCases =
            new ArrayList<>();

    public ComplaintAnalysisController() {
        addCase("CC101", "Fake bank link and OTP theft",
                "Phishing", 15000);
        addCase("CC102", "Unauthorized money transfer",
                "Financial Fraud", 45000);
        addCase("CC103", "Identity theft and impersonation",
                "Identity Theft", 5000);
        addCase("CC104", "Fake phishing message stole bank credentials",
                "Phishing", 8000);
        addCase("CC105", "Online account hacked",
                "Account Compromise", 12000);
    }

    private void addCase(String id, String description,
                         String category, double loss) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("caseId", id);
        c.put("description", description);
        c.put("category", category);
        c.put("lossAmount", loss);
        historicalCases.add(c);
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(
            @RequestBody Map<String, Object> complaint) {

        String originalDescription = String.valueOf(
                complaint.getOrDefault("description", "")
        );

        String description = originalDescription.toLowerCase(
                Locale.ROOT);

        double loss = 0;
        try {
            loss = Double.parseDouble(
                    String.valueOf(
                            complaint.getOrDefault("lossAmount", 0)
                    )
            );
            if (!Double.isFinite(loss) || loss < 0) {
                loss = 0;
            }
        } catch (NumberFormatException ignored) {
            loss = 0;
        }

        String category = classify(description);
        List<String> factors = new ArrayList<>();

        factors.add("Category assigned using rule-based keyword matching: "
                + category);

        String priority;
        if (loss >= 50000) {
            priority = "HIGH";
            factors.add("Reported financial loss is at least 50000");
        } else if (loss >= 10000) {
            priority = "MEDIUM";
            factors.add("Reported financial loss is at least 10000");
        } else {
            priority = "LOW";
            factors.add("Reported financial loss is below 10000");
        }

        if (description.contains("ongoing")
                || description.contains("currently")
                || description.contains("still accessing")) {
            factors.add("Description may indicate an ongoing incident; verify urgently");
            if (!"HIGH".equals(priority)) {
                priority = "HIGH";
            }
        }

        Set<String> inputWords = words(description);
        List<Map<String, Object>> related = new ArrayList<>();

        for (Map<String, Object> old : historicalCases) {
            String oldDescription = String.valueOf(
                    old.get("description")).toLowerCase(Locale.ROOT);

            Set<String> oldWords = words(oldDescription);
            Set<String> common = new HashSet<>(inputWords);
            common.retainAll(oldWords);

            Set<String> union = new HashSet<>(inputWords);
            union.addAll(oldWords);

            double similarity = union.isEmpty() ? 0 :
                    (double) common.size() / union.size();

            boolean sameCategory =
                    category.equals(old.get("category"));

            if (sameCategory || similarity >= 0.15) {
                Map<String, Object> match = new LinkedHashMap<>();
                match.put("caseId", old.get("caseId"));
                match.put("category", old.get("category"));
                match.put("description", old.get("description"));
                match.put("historicalLossAmount", old.get("lossAmount"));
                match.put("similarityPercent",
                        Math.round(similarity * 100));
                match.put("sameCategory", sameCategory);

                List<String> matchFactors = new ArrayList<>();
                if (sameCategory) {
                    matchFactors.add("Same predicted crime category");
                }
                if (!common.isEmpty()) {
                    matchFactors.add("Shared terms: "
                            + String.join(", ", common));
                }
                match.put("matchFactors", matchFactors);
                match.put("assessment",
                        sameCategory && similarity >= 0.15
                                ? "Category and text overlap"
                                : sameCategory
                                ? "Category match only"
                                : "Text overlap only");

                related.add(match);
            }
        }

        related.sort((a, b) ->
                Long.compare(
                        ((Number) b.get("similarityPercent")).longValue(),
                        ((Number) a.get("similarityPercent")).longValue()
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", category);
        result.put("priority", priority);
        result.put("lossAmount", loss);
        result.put("keyFactors", factors);
        result.put("relatedCases", related);
        result.put("analysisMethod",
                "Rule-based classification and Jaccard text similarity");
        result.put("historicalDataSource",
                "Five illustrative cases embedded in the AI service; not an external dataset");
        result.put("note",
                "Preliminary decision support only. Similarity is not proof that cases are connected. Human investigation is required.");

        return result;
    }

    private String classify(String description) {
        if (description.contains("phishing")
                || description.contains("fake link")
                || description.contains("otp")
                || description.contains("bank credentials")) {
            return "Phishing";
        }

        if (description.contains("identity theft")
                || description.contains("impersonation")) {
            return "Identity Theft";
        }

        if (description.contains("account hacked")
                || description.contains("account compromised")) {
            return "Account Compromise";
        }

        if (description.contains("fraud")
                || description.contains("unauthorized transaction")
                || description.contains("unauthorized money")
                || description.contains("money stolen")
                || description.contains("money transfer")
                || description.contains("transaction")) {
            return "Financial Fraud";
        }

        return "Other Cybercrime";
    }

    private Set<String> words(String text) {
        Set<String> result = new HashSet<>(
                Arrays.asList(text.split("[^a-z0-9]+"))
        );

        result.removeAll(Arrays.asList(
                "", "a", "an", "the", "and", "or", "is", "was",
                "were", "to", "from", "in", "of", "my", "i",
                "it", "that", "their", "they", "victim",
                "received", "reported", "with", "for", "on",
                "this", "through", "asked", "enter", "lost"
        ));

        return result;
    }
}
