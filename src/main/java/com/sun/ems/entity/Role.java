package com.sun.ems.entity;

import com.sun.ems.constant.RoleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 20, unique = true)
    private RoleType name;
    
    private String description;
}
