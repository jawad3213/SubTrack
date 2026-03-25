package com.subtrack.dao;

import com.subtrack.entity.User;
import com.subtrack.enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDAO {
    void create(User user);
    void update(User user);
    void delete(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    List<User> findByRole(Role role);
    boolean existsByEmail(String email);
}
