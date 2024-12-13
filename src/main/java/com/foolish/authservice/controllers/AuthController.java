package com.foolish.authservice.controllers;

import com.foolish.authservice.mappers.AccountMapper;
import com.foolish.authservice.model.Account;
import com.foolish.authservice.model.RefreshToken;
import com.foolish.authservice.DTOs.AccountDTO;
import com.foolish.authservice.records.LoginRequest;
import com.foolish.authservice.records.ROLE;
import com.foolish.authservice.response.ResponseData;
import com.foolish.authservice.response.ResponseError;
import com.foolish.authservice.services.AccountService;
import com.foolish.authservice.services.RefreshTokenService;
import com.foolish.authservice.services.RoleService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/v1/auth")
public class AuthController {

  private final AccountService accountService;
  private final PasswordEncoder passwordEncoder;
  private final RoleService roleService;
  private final AuthenticationManager authenticationManager;
  private final Environment env;
  private final RefreshTokenService refreshTokenService;

  @PostMapping("/register")
  public ResponseEntity<ResponseData> register(@RequestBody @Valid Account account) {
    // Hàm tạo một Account mới bên trong hệ thống.
    AccountDTO isExisted = accountService.findAccountDTOByUsername(account.getUsername());
    if (accountService.findAccountByUsername(account.getUsername()) != null || accountService.findAccountByUsername(account.getEmail()) != null)
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.CONFLICT.value(), "Account already exists"));
    // Không tìm thấy Username. Tạo Account mới.
    account.setRole(roleService.findByRoleName(ROLE.USER));
    account.setPassword(passwordEncoder.encode(account.getPassword()));
    try {
      account = accountService.save(account);
    } catch (RuntimeException e) {
      log.error("Failed to create user: {}", account.getUsername(), e);
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to register a new user"));
    }

    if (account.getId() > 0)
      return ResponseEntity.ok().body(new ResponseData(HttpStatus.CREATED.value(), "Created successfully", null));
    return ResponseEntity.ok().body(new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to register a new user"));
  }

  @PostMapping("/login")
  public ResponseEntity<ResponseData> login(@RequestBody LoginRequest loginRequest) {
    String accessToken = "";
    String content = "";
    Authentication authentication = UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.username(), loginRequest.password());
    Authentication authenticationResponse = authenticationManager.authenticate(authentication); // Thực hiện authenticate bằng cách dùng @Bean Manager đã tạo trong ProjectConfigSecurity để xác thực.

    if (authenticationResponse != null && authenticationResponse.isAuthenticated()) {
      // Thực hiện việc tạo access-token và refresh-token.
      Account account = accountService.findAccountByUsername(authenticationResponse.getName());
      String username = authenticationResponse.getName();
      content = RefreshTokenService.generateRefreshToken(username); // Vì mã hoá thông tin dựa vào username nên chuỗi mã hoá mặc định là duy nhất.

      RefreshToken token = refreshTokenService.findByContent(content);
      if (token == null) {
        token = new RefreshToken();
        token.setContent(content);
        token.setUsername(username);
        token.setValidUntil(new Timestamp(new Date(new Date().getTime() + 30 * 24 * 3600 * 1000L).getTime()));  // Có thời hạn 30 ngày.
      }
      String secret = env.getProperty("SECRET_KEY");
      SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
      accessToken = Jwts.builder().setIssuer("Authorization server").setSubject("Access Token").claim("user_id", account.getId()).setIssuedAt(new Date()).setExpiration(new Date((new Date()).getTime() + 7 * 24 * 3600 * 1000L)).signWith(secretKey).compact();
      token.setAccessToken(accessToken);
      refreshTokenService.save(token);
    } else {
      log.error("Unauthenticated user!");
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.UNAUTHORIZED.value(), "Unauthenticated user!"));
    }
    // Thống nhất là sẽ chỉ gửi access-token và refresh-token ở cookies. access-token sẽ có thời hạn là 7 days, refresh-token là 30 days.
    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token").value(content).httpOnly(true).path("/").maxAge(30 * 24 * 3600L).build();

    // Thống nhất không gửi dto.
    return ResponseEntity.ok().header("Set-Cookie", refreshCookie.toString()).body(new ResponseData(HttpStatus.OK.value(), "Login successfully", Map.of("token_type", "Bearer", "access_token", accessToken, "expires_in", new Timestamp(new Date().getTime() + 7 * 24 * 3600 * 1000L))));
  }

  // Tạo ra API để refresh access token.
  @GetMapping("/refresh-token")
  public ResponseEntity<ResponseData> refreshToken(HttpServletRequest request) {
    StringBuilder refreshToken = new StringBuilder();
    Cookie[] cookies = request.getCookies();
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals("refresh_token")) {
        // Tìm thấy được refresh_token.
        refreshToken.append(cookie.getValue());
        break;
      }
    }
    if (!StringUtils.hasText(refreshToken)) {
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Refresh token mustn't be empty!"));
    }
    String value = request.getHeader("Authorization");
    String accessToken = value.substring(7); // Bỏ qua Bearer.
    if (accessToken.isEmpty())
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Access token mustn't be empty!"));

    // Xác thực xem access-token đã hết hạn hay chưa.
    RefreshToken token = null;
    String secret = env.getProperty("SECRET_KEY");
    SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    boolean isExpired = false;

    try {
      Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(accessToken.toString()).getBody();
    } catch (ExpiredJwtException e) {
      // Token thực sự hết hạn.
      log.warn("Access token has truly expired");
      token = refreshTokenService.findByContent(refreshToken.toString());
      isExpired = token.getAccessToken().contentEquals(accessToken);
    } catch (RuntimeException e) {
      throw new RuntimeException("Validate JWT token failed!");
    }
    if (!isExpired) {
      return ResponseEntity.status(HttpStatus.OK).body(new ResponseError(HttpStatus.FORBIDDEN.value(), "Access token didn't expire!"));
    }
    // Phải xét 2 trường hợp: refresh token còn hạn và refresh token hết hạn.

    // 1. Xét trường hợp refresh token còn hạn.
    Timestamp current = new Timestamp(new Date().getTime());
    if (token.getId() > 0 && token.getValidUntil().getTime() > current.getTime()) {
      // Trả về cho Client một access-token mới.
      AccountDTO dto = accountService.findAccountDTOByUsername(token.getUsername());
      String jwt = Jwts.builder().setIssuer("Authorization Server").setSubject("Access Token").claim("user_id", dto.getId()).setExpiration(new Date((new Date()).getTime() + 7 * 24 * 3600 * 1000L)).signWith(secretKey).compact();

      // Update lại jwt ở Tokens trong DB.
      token.setAccessToken(jwt);
      return ResponseEntity.ok(new ResponseData(HttpStatus.OK.value(), "Issued a new access token!", Map.of("token_type", "Bearer", "access_token", jwt, "expires_in", new Timestamp(new Date().getTime() + 7 * 24 * 3600 * 1000L))));
    }
    // 2. Xét trường hợp là hết hạn, xoá refresh token dưới DB rồi sau đó trả về response yêu cầu Client đăng nhập lại.
    refreshTokenService.deleteRefreshTokenByContent(refreshToken.toString());
    // deleted successfully.
    return ResponseEntity.ok().body(new ResponseError(HttpStatus.UNAUTHORIZED.value(), "refresh token is expired, please login again!"));
  }

  @GetMapping("/logout")
  public ResponseEntity<ResponseData> logout(HttpServletRequest request) {
    StringBuilder refreshToken = new StringBuilder();
    Cookie[] cookies = request.getCookies();
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals("refresh_token")) {
        // Tìm thấy được refresh_token.
        refreshToken.append(cookie.getValue());
        break;
      }
    }
    if (!StringUtils.hasText(refreshToken)) {
      return ResponseEntity.ok().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Refresh token mustn't be empty!"));
    }
    RefreshToken token = refreshTokenService.findByContent(refreshToken.toString());
    if (token == null) {
      return ResponseEntity.status(HttpStatus.OK).body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Can't logout!"));
    }
    // Nếu tìm thấy token thì xoá access token có liên quan.
    String value = request.getHeader("Authorization");
    String accessToken = value.substring(7);
    if (accessToken.isEmpty() || token.getContent().isEmpty() || !accessToken.equals(token.getAccessToken()))
      return ResponseEntity.ok(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Can't logout!"));

    refreshTokenService.delete(token);
    ResponseCookie cookie = ResponseCookie.from("refresh_token", "").path("/").maxAge(0).build();

    return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(new ResponseData(HttpStatus.OK.value(), "Logout successfully!", null));
  }
}
