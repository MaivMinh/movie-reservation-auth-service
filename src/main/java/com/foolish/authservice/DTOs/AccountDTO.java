package com.foolish.authservice.DTOs;

import com.foolish.authservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
  private Integer id;
  private String username;
  private Role role;
}
