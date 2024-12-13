package com.foolish.authservice.services;

import com.foolish.authservice.mappers.AccountMapper;
import com.foolish.authservice.model.Account;
import com.foolish.authservice.DTOs.AccountDTO;
import com.foolish.authservice.model.Role;
import com.foolish.authservice.records.ROLE;
import com.foolish.authservice.repository.AccountRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepo accountRepo;
  private final Environment environment;
  private final AccountMapper accountMapper;


  public AccountDTO findAccountDTOByUsername(String username) {
    return accountMapper.toDTO(accountRepo.findByUsername(username));
  }

  public Account save(Account account) {
    return accountRepo.save(account);
  }

  public Account findAccountByUsername(String username) {
    return accountRepo.findByUsername(username);
  }

  public Role doIntrospect(String userId) {
    // Thực hiện tìm kiếm ROLE của user.
    Account account = accountRepo.findAccountById(Integer.parseInt(userId));
    if (account != null && account.getId() > 0) {
      return account.getRole();
    }
    return null;
  }

  public Account findAccountByEmail(String email) {
    return accountRepo.findByEmail(email);
  }
}
