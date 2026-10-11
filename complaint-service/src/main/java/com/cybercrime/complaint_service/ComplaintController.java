
package com.cybercrime.complaint_service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintRepository repository;
    private final RestClient restClient;

    private static final Set<String> STOP_WORDS = new HashSet<>(
            Arrays.asList(
                    "a", "an", "the", "and", "or", "is", "was",
                    "were", "are", "to", "from", "in", "of", "my",
                    "i", "it", "that", "their", "they", "with",
                    "for", "on", "this", "through", "asked",
                    "enter", "lost", "after", "before", "about",
                    "have", "has", "had", "can", "could", "would",
                    "should", "me", "we", "you", "he", "she",
                    "them", "our", "your", "as", "at", "by",
                    "be", "been", "being", "into", "received",
                    "reported", "complaint", "case", "investigation",
                    "request", "provide", "provided", "suspect",
                    "evidence", "records", "screenshots"
            )
    );

    public ComplaintController(ComplaintRepository repository) {
        this.repository = repository;
        this.restClient = RestClient.create();
    }

    // Retrieve all complaints
    @GetMapping
    public List<Complaint> getAll() {
        return repository.findAll();
    }

    // Register a complaint and request AI classification
    @PostMapping
    public Complaint create(@RequestBody Complaint complaint) {

        Map<String, Object> request = new HashMap<>();
        request.put("description", complaint.getDescription());

        Double lossAmount = complaint.getLossAmount();
        request.put(
                "lossAmount",
                lossAmount != null ? lossAmount : 0.0
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri("http://localhost:8090/api/ai/analyze")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                Object category = response.get("category");
                Object priority = response.get("priority");

                if (category != null) {
                    complaint.setCategory(category.toString());
                }

                if (priority != null) {
                    complaint.setPriority(priority.toString());
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "AI analysis unavailable: " + e.getMessage()
            );

            complaint.setCategory("Pending AI analysis");
            complaint.setPriority("Pending AI analysis");
        }

        Complaint saved = repository.save(complaint);

        System.out.println("Saved complaint ID: " + saved.getId());

        return saved;
    }

    // Retrieve one complaint by ID
    @GetMapping("/{id}")
    public Complaint getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Complaint not found: " + id
                        )
                );
    }

    // Analyze a complaint without saving it
    @PostMapping("/analyze")
    public Map<?, ?> analyzeComplaint(
            @RequestBody Map<String, Object> request) {

        return restClient.post()
                .uri("http://localhost:8090/api/ai/analyze")
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    // Compare a complaint with historical complaints
    @GetMapping("/related/{id}")
    public Map<String, Object> getRelatedComplaints(
            @PathVariable Long id) {

        Complaint current = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Complaint not found: " + id
                        )
                );

        List<Complaint> allComplaints = repository.findAll();
        List<Map<String, Object>> related = new ArrayList<>();

        Set<String> currentWords = words(current.getDescription());

        for (Complaint other : allComplaints) {

            if (current.getId().equals(other.getId())) {
                continue;
            }

            Set<String> otherWords = words(other.getDescription());

            Set<String> common = new HashSet<>(currentWords);
            common.retainAll(otherWords);

            Set<String> union = new HashSet<>(currentWords);
            union.addAll(otherWords);

            int similarity = union.isEmpty()
                    ? 0
                    : (int) Math.round(
                            100.0 * common.size() / union.size()
                    );

            boolean sameCategory =
                    current.getCategory() != null
                    && other.getCategory() != null
                    && !current.getCategory().equalsIgnoreCase(
                            "Pending AI analysis"
                    )
                    && !other.getCategory().equalsIgnoreCase(
                            "Pending AI analysis"
                    )
                    && current.getCategory().equalsIgnoreCase(
                            other.getCategory()
                    );

            // Category alone is not enough to include a match
            if (similarity >= 15) {

                Map<String, Object> match = new LinkedHashMap<>();

                match.put("id", other.getId());
                match.put("category", other.getCategory());
                match.put("priority", other.getPriority());
                match.put("lossAmount", other.getLossAmount());
                match.put("description", other.getDescription());
                match.put("similarityPercent", similarity);
                match.put("sameCategory", sameCategory);

                match.put(
                        "sharedTerms",
                        common.stream()
                                .sorted()
                                .collect(Collectors.toList())
                );

                List<String> factors = new ArrayList<>();
                factors.add("Meaningful description terms overlap");

                if (sameCategory) {
                    factors.add("Same recorded complaint category");
                }

                if (current.getLossAmount() != null
                        && other.getLossAmount() != null
                        && current.getLossAmount() > 0
                        && current.getLossAmount().equals(
                                other.getLossAmount()
                        )) {
                    factors.add("Same recorded loss amount");
                }

                if (!common.isEmpty()) {
                    factors.add(
                            "Shared terms: "
                                    + common.stream()
                                            .sorted()
                                            .collect(Collectors.joining(", "))
                    );
                }

                match.put("matchFactors", factors);
                related.add(match);
            }
        }

        related.sort((a, b) ->
                Integer.compare(
                        (Integer) b.get("similarityPercent"),
                        (Integer) a.get("similarityPercent")
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("complaintId", current.getId());
        result.put("relatedComplaints", related);
        result.put(
                "analysisMethod",
                "Rule-based text similarity using filtered keywords, "
                        + "with category and loss amount as supporting factors"
        );
        result.put(
                "note",
                "Potential matches only. Similarity does not prove "
                        + "that cases are connected or evidence is authentic."
        );

        return result;
    }

    // Normalize descriptions and remove common words and numbers
    private Set<String> words(String description) {

        Set<String> result = new HashSet<>();

        if (description == null || description.isBlank()) {
            return result;
        }

        String normalized = description.toLowerCase(Locale.ROOT);

        // Remove email addresses
        normalized = normalized.replaceAll(
                "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}",
                " "
        );

        // Remove numeric fragments, dates and amounts
        normalized = normalized.replaceAll("\\b\\d+\\b", " ");

        Matcher matcher = Pattern.compile("[a-z]{3,}")
                .matcher(normalized);

        while (matcher.find()) {
            String word = matcher.group();

            if (!STOP_WORDS.contains(word)) {
                result.add(word);
            }
        }

        return result;
    }

    // Delete a complaint
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
