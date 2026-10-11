
package com.cybercrime.complaint_service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByComplaintId(Long complaintId);

}
