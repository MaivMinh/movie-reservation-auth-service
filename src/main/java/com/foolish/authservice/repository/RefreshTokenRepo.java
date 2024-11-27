package com.foolish.authservice.repository;

import com.foolish.authservice.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Integer> {
  RefreshToken findByContent(String refreshToken);

  void deleteByContent(String content);
}
