
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
@RequestMapping("/api/investigations")
@CrossOrigin(origins = "*")
public class InvestigationController {

    private final InvestigationRepository investigationRepository;
    private final ComplaintRepository complaintRepository;

    private static final Set<String> VALID_STATUSES = Set.of(
            "NOT_STARTED",
            "IN_PROGRESS",
            "AWAITING_EVIDENCE",
            "RESOLVED"
    );

    public InvestigationController(
            InvestigationRepository investigationRepository,
            ComplaintRepository complaintRepository) {
        this.investigationRepository = investigationRepository;
        this.complaintRepository = complaintRepository;
    }

    @GetMapping
    public List<Investigation> getAll() {
        return investigationRepository.findAll();
    }

    @GetMapping("/{id}")
    public Investigation getById(@PathVariable Long id) {
        return investigationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Investigation not found: " + id));
    }

    @PostMapping
    public Investigation create(
            @RequestBody Map<String, Object> request) {

        Object complaintIdValue = request.get("complaintId");

        if (complaintIdValue == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "complaintId is required");
        }

        Long complaintId;

        try {
            complaintId = Long.valueOf(complaintIdValue.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "complaintId must be a valid number");
        }

        if (!complaintRepository.existsById(complaintId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Complaint not found: " + complaintId);
        }

        if (investigationRepository.existsByComplaintId(complaintId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An investigation already exists for complaint "
                            + complaintId);
        }

        Investigation investigation = new Investigation();
        investigation.setComplaintId(complaintId);
        investigation.setStatus("NOT_STARTED");

        return investigationRepository.save(investigation);
    }

    @PutMapping("/{id}")
    public Investigation update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Investigation investigation =
                investigationRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Investigation not found: " + id));

        if (request.containsKey("status")) {
            Object value = request.get("status");

            if (value == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "status cannot be empty");
            }

            String status = value.toString()
                    .trim()
                    .toUpperCase()
                    .replace(' ', '_');

            if (!VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid status. Allowed: " + VALID_STATUSES);
            }

            investigation.setStatus(status);
        }

        if (request.containsKey("assignedOfficer")) {
            investigation.setAssignedOfficer(
                    readOptionalString(request, "assignedOfficer"));
        }

        if (request.containsKey("notes")) {
            investigation.setNotes(readOptionalString(request, "notes"));
        }

        if (request.containsKey("nextAction")) {
            investigation.setNextAction(
                    readOptionalString(request, "nextAction"));
        }

        return investigationRepository.save(investigation);
    }

    private String readOptionalString(
            Map<String, Object> request, String field) {

        Object value = request.get(field);
        if (value == null) {
            return null;
        }

        return value.toString().trim();
    }
}
