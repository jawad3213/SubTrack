package com.subtrack.entity;

import com.subtrack.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
public class Admin extends User {

    public Admin() {
        this.setRole(Role.ADMIN);
    }
}
