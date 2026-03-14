package org.darius.authservice.repositories;

import org.darius.authservice.entities.Users;
import org.darius.authservice.entities.Validation;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Optional;

public interface ValidationRepository extends CrudRepository<Validation, Integer> {

    Optional<Validation> findByCode(String code);

    void deleteAllByExpirationBefore(Instant instant);

    Validation findByUser(Users user);
}
