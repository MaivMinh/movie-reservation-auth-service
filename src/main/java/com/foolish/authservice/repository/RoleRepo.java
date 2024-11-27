package com.foolish.authservice.repository;

import com.foolish.authservice.model.Role;
import com.foolish.authservice.records.ROLE;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepo extends JpaRepository<Role, Integer> {
  Role findByName(ROLE name);
}
