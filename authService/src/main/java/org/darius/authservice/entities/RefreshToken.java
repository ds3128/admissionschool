package org.darius.authservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private boolean expire;
    @Column(nullable = false, length = 2048)
    private String value;
    private Instant createdAt;
    private Instant expiredAt;

}
