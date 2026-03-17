package org.darius.authservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.authservice.common.enums.RoleType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

}
