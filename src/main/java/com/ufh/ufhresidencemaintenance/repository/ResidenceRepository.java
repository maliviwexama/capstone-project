package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.Residence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidenceRepository extends JpaRepository<Residence, Long> {
}