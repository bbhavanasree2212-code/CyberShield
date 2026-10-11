
package com.cybercrime.complaint_service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigationRepository
        extends JpaRepository<Investigation, Long> {

    List<Investigation> findByComplaintId(Long complaintId);

    boolean existsByComplaintId(Long complaintId);
}

