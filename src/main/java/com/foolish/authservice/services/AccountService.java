package com.foolish.authservice.services;

import com.foolish.authservice.mappers.AccountMapper;
import com.foolish.authservice.model.Account;
import com.foolish.authservice.DTOs.AccountDTO;
import com.foolish.authservice.repository.AccountRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepo accountRepo;
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
}
