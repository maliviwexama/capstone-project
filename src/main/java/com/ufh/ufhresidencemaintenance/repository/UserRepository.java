package com.ufh.ufhresidencemaintenance.repository;

import com.ufh.ufhresidencemaintenance.model.User;
import com.ufh.ufhresidencemaintenance.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByVerificationStatus(VerificationStatus status);
    boolean existsByStudentNumber(String studentNumber);
    boolean existsByStaffId(String staffId);
}
