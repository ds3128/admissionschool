package org.darius.course.repositories;

import org.darius.course.entities.Enrollment;
import org.darius.course.enums.EnrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(String studentId);

    List<Enrollment> findByStudentIdAndSemester_Id(String studentId, Long semesterId);

    List<Enrollment> findByStudentIdAndSemester_IdAndStatus(
            String studentId, Long semesterId, EnrollStatus status
    );

    List<Enrollment> findByMatiere_IdAndSemester_Id(Long matiereId, Long semesterId);

    Optional<Enrollment> findByStudentIdAndMatiere_IdAndSemester_Id(
            String studentId, Long matiereId, Long semesterId
    );

    boolean existsByStudentIdAndMatiere_IdAndSemester_Id(
            String studentId, Long matiereId, Long semesterId
    );

    // Tous les étudiants inscrits à une matière pour un semestre
    List<Enrollment> findByMatiere_IdAndSemester_IdAndStatus(
            Long matiereId, Long semesterId, EnrollStatus status
    );
}
