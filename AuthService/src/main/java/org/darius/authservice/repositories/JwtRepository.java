package org.darius.authservice.repositories;

import org.darius.authservice.entities.Jwt;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface JwtRepository extends CrudRepository<Jwt, Integer> {
    Optional<Jwt> findJwtByValueAndDeactivateAndExpire(String token, boolean deactivate, boolean expire);

    @Query("FROM Jwt j WHERE j.expire = :expire and j.deactivate = :deactivate and j.user.email= :email")
    Optional<Jwt> findUserValidToken(String email, boolean deactivate, boolean expire);

    @Query("FROM Jwt j WHERE j.user.email= :email")
    Stream<Jwt> findUserByEmail(String email);

    @Query("FROM Jwt j WHERE j.refreshToken.value = :value")
    Optional<Jwt> findUserByRefreshToken(String value);

    void deleteAllByExpireAndDeactivate(boolean expire, boolean deactivate);
}
