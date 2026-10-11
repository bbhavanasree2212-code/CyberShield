
package com.cybercrime.complaint_service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/evidence")
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceRepository evidenceRepository;
    private final ComplaintRepository complaintRepository;

    private static final Set<String> VALID_STATUSES = Set.of(
            "COLLECTED",
            "UNDER_REVIEW",
            "VERIFIED",
            "REJECTED"
    );

    public EvidenceController(
            EvidenceRepository evidenceRepository,
            ComplaintRepository complaintRepository) {
        this.evidenceRepository = evidenceRepository;
        this.complaintRepository = complaintRepository;
    }

    @GetMapping
    public List<Evidence> getAll() {
        return evidenceRepository.findAll();
    }

    @GetMapping("/{id}")
    public Evidence getById(@PathVariable Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evidence not found: " + id));
    }

    @GetMapping("/complaint/{complaintId}")
    public List<Evidence> getByComplaint(@PathVariable Long complaintId) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Complaint not found: " + complaintId);
        }

        return evidenceRepository.findByComplaintId(complaintId);
    }

    @PostMapping
    public Evidence create(@RequestBody Map<String, Object> request) {
        Long complaintId = readRequiredLong(request, "complaintId");

        if (!complaintRepository.existsById(complaintId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Complaint not found: " + complaintId);
        }

        Evidence evidence = new Evidence();

        evidence.setComplaintId(complaintId);
        evidence.setEvidenceName(readRequiredString(request, "evidenceName"));
        evidence.setEvidenceType(readOptionalString(request, "evidenceType"));
        evidence.setReferenceNumber(readOptionalString(request, "referenceNumber"));
        evidence.setSourceUrl(readOptionalString(request, "sourceUrl"));
        evidence.setEmailInformation(readOptionalString(request, "emailInformation"));
        evidence.setDocumentReference(readOptionalString(request, "documentReference"));
        evidence.setDescription(readOptionalString(request, "description"));
        evidence.setCollectedBy(readOptionalString(request, "collectedBy"));
        evidence.setStorageLocation(readOptionalString(request, "storageLocation"));
        evidence.setStatus("COLLECTED");

        return evidenceRepository.save(evidence);
    }

    @PutMapping("/{id}")
    public Evidence update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evidence not found: " + id));

        if (request.containsKey("evidenceName")) {
            evidence.setEvidenceName(
                    readRequiredString(request, "evidenceName"));
        }

        if (request.containsKey("evidenceType")) {
            evidence.setEvidenceType(
                    readOptionalString(request, "evidenceType"));
        }

        if (request.containsKey("referenceNumber")) {
            evidence.setReferenceNumber(
                    readOptionalString(request, "referenceNumber"));
        }

        if (request.containsKey("sourceUrl")) {
            evidence.setSourceUrl(
                    readOptionalString(request, "sourceUrl"));
        }

        if (request.containsKey("emailInformation")) {
            evidence.setEmailInformation(
                    readOptionalString(request, "emailInformation"));
        }

        if (request.containsKey("documentReference")) {
            evidence.setDocumentReference(
                    readOptionalString(request, "documentReference"));
        }

        if (request.containsKey("description")) {
            evidence.setDescription(
                    readOptionalString(request, "description"));
        }

        if (request.containsKey("collectedBy")) {
            evidence.setCollectedBy(
                    readOptionalString(request, "collectedBy"));
        }

        if (request.containsKey("storageLocation")) {
            evidence.setStorageLocation(
                    readOptionalString(request, "storageLocation"));
        }

        if (request.containsKey("status")) {
            String status = readRequiredString(request, "status")
                    .toUpperCase()
                    .replace(' ', '_');

            if (!VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid status. Allowed: " + VALID_STATUSES);
            }

            evidence.setStatus(status);
        }

        return evidenceRepository.save(evidence);
    }

    private Long readRequiredLong(
            Map<String, Object> request, String field) {

        Object value = request.get(field);

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " is required");
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a valid number");
        }
    }

    private String readRequiredString(
            Map<String, Object> request, String field) {

        Object value = request.get(field);

        if (value == null || value.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " is required");
        }

        return value.toString().trim();
    }

    private String readOptionalString(
            Map<String, Object> request, String field) {

        Object value = request.get(field);

        if (value == null) {
            return null;
        }

        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
