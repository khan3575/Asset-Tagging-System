package com.sil.asset_tagging_system.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;
}
