package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserId(Long userId);
}
