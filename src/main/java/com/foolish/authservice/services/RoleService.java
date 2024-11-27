package com.foolish.authservice.services;

import com.foolish.authservice.model.Role;
import com.foolish.authservice.records.ROLE;
import com.foolish.authservice.repository.RoleRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoleService {
  private final RoleRepo roleRepo;

  public Role findByRoleName(ROLE name) {
    return roleRepo.findByName(name);
  }
}
