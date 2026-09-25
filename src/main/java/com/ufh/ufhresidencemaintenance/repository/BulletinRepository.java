package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.Bulletin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BulletinRepository extends JpaRepository<Bulletin, Long> {
    List<Bulletin> findByActiveTrueOrderByCreatedAtDesc();
    List<Bulletin> findAllByOrderByCreatedAtDesc();
    List<Bulletin> findTop3ByActiveTrueOrderByCreatedAtDesc();
}
