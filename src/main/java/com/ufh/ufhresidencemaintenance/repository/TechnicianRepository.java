package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
}
