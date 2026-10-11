
package com.cybercrime.complaint_service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class HistoricalComplaintService {

    private final ComplaintRepository complaintRepository;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "this", "that",
            "was", "were", "have", "has", "had", "into", "about",
            "complaint", "fraud", "case", "reported", "report",
            "request", "requested", "investigation", "investigate",
            "suspect", "suspected", "provide", "provided", "providing",
            "evidence", "records", "record", "screenshots", "screenshot",
            "online", "after", "before", "during", "through", "their",
            "there", "they", "them", "then", "when", "where", "while",
            "onto", "upon", "also", "some", "been", "being",
            "will", "would", "could", "should", "can", "you", "your",
            "our", "out", "not", "but", "are", "who", "what",
            "which", "how", "all", "any", "lost", "money",
            "someone", "received", "asked", "click"
    );

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[a-z]{3,}");

    public HistoricalComplaintService(
            ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public List<Map<String, Object>> findRelatedComplaints(
            Long complaintId) {

        Complaint current = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException(
                        "Complaint not found: " + complaintId));

        List<Complaint> historicalComplaints =
                complaintRepository.findAll();

        Set<String> currentWords =
                extractWords(current.getDescription());

        List<Map<String, Object>> results = new ArrayList<>();

        for (Complaint historical : historicalComplaints) {

            // Do not compare a complaint with itself.
            if (Objects.equals(current.getId(), historical.getId())) {
                continue;
            }

            Set<String> historicalWords =
                    extractWords(historical.getDescription());

            // Find terms shared by both complaints.
            Set<String> commonWords = new HashSet<>(currentWords);
            commonWords.retainAll(historicalWords);

            // Find all unique terms from both complaints.
            Set<String> allWords = new HashSet<>(currentWords);
            allWords.addAll(historicalWords);

            // Jaccard similarity: shared terms / all unique terms.
            int similarityScore = allWords.isEmpty()
                    ? 0
                    : (int) Math.round(
                            100.0 * commonWords.size() / allWords.size());

            boolean sameCategory =
                    isKnownCategory(current.getCategory())
                    && isKnownCategory(historical.getCategory())
                    && current.getCategory().trim().equalsIgnoreCase(
                            historical.getCategory().trim());

            boolean sameLossAmount =
                    current.getLossAmount() != null
                    && historical.getLossAmount() != null
                    && current.getLossAmount() > 0
                    && current.getLossAmount().equals(
                            historical.getLossAmount());

            // Text overlap is required; category and amount alone
            // must not make unrelated complaints appear related.
            if (similarityScore < 15) {
                continue;
            }

            Map<String, Object> match = new LinkedHashMap<>();

            match.put("complaintId", historical.getId());
            match.put("category", historical.getCategory());
            match.put("priority", historical.getPriority());
            match.put("lossAmount", historical.getLossAmount());
            match.put("description", historical.getDescription());
            match.put("similarityScore", similarityScore);

            List<String> matchingFactors = new ArrayList<>();

            matchingFactors.add(
                    "Meaningful description terms overlap");

            if (sameCategory) {
                matchingFactors.add(
                        "Same recorded complaint category");
            }

            if (sameLossAmount) {
                matchingFactors.add(
                        "Same recorded loss amount");
            }

            matchingFactors.add(
                    "Shared terms: " + commonWords.stream()
                            .sorted()
                            .collect(Collectors.joining(", ")));

            match.put("matchingFactors", matchingFactors);

            results.add(match);
        }

        // Show the highest text-similarity matches first.
        results.sort(
                Comparator.comparingInt(
                        (Map<String, Object> result) ->
                                (Integer) result.get("similarityScore"))
                        .reversed());

        return results;
    }

    private boolean isKnownCategory(String category) {
        return category != null
                && !category.isBlank()
                && !category.equalsIgnoreCase("Pending AI analysis");
    }

    private Set<String> extractWords(String description) {

        Set<String> words = new HashSet<>();

        if (description == null || description.isBlank()) {
            return words;
        }

        String normalized =
                description.toLowerCase(Locale.ROOT);

        // Ignore email addresses.
        normalized = normalized.replaceAll(
                "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}",
                " ");

        // Ignore numeric fragments such as dates and amounts.
        normalized = normalized.replaceAll("\\b\\d+\\b", " ");

        Matcher matcher = WORD_PATTERN.matcher(normalized);

        while (matcher.find()) {
            String word = matcher.group();

            if (!STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }

        return words;
    }
}
