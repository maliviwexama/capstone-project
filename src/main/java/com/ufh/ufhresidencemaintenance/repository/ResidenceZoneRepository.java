package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.ResidenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResidenceZoneRepository extends JpaRepository<ResidenceZone, Long> {
    Optional<ResidenceZone> findByActiveTrue();
}
