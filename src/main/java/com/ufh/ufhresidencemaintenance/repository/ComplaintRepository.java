package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.Complaint;
import com.ufh.ufhresidencemaintenance.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserId(Long userId);

    List<Complaint> findByArchivedFalseAndStatusNot(ComplaintStatus status);

    List<Complaint> findByArchivedFalseAndStatusOrderByResolvedAtDesc(ComplaintStatus status);

    List<Complaint> findByArchivedTrueOrderByArchivedAtDesc();

    List<Complaint> findByArchivedFalse();

    long countByArchivedFalseAndStatus(ComplaintStatus status);
    long countByArchivedFalseAndStatusNot(ComplaintStatus status);
}
