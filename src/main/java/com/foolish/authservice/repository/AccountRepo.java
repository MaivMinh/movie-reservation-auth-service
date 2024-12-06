package com.foolish.authservice.repository;

import com.foolish.authservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepo extends JpaRepository<Account, Integer> {
  Account findByUsername(String username);
  Account findByEmail(String email);
  Account findAccountById(Integer id);
}
