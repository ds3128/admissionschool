package org.darius.authservice.repositories;

import org.darius.authservice.entities.Users;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<Users, String> {
    Optional<Users> findByEmail(String email);

    boolean existsByInstitutionalEmail(String institutionalEmail);
}
