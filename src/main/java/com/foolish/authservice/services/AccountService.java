package com.foolish.authservice.services;

import com.foolish.authservice.mappers.AccountMapper;
import com.foolish.authservice.model.Account;
import com.foolish.authservice.DTOs.AccountDTO;
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

  public Claims doIntrospect(String token) {
    // Thực hiện xác thực jwt token ở trong này.
    String secret = environment.getProperty("SECRET_KEY"); // Replace with your actual secret key
    SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    Claims claims;
    try {
      claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    } catch (RuntimeException e) {
      return null;
    }
    return claims;
  }

  public Account findAccountByEmail(String email) {
    return accountRepo.findByEmail(email);
  }
}
