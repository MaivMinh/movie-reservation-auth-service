package com.foolish.authservice.mappers;

import com.foolish.authservice.model.Account;
import com.foolish.authservice.DTOs.AccountDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
  AccountDTO toDTO(Account account);
}
