package com.foolish.authservice.config;

import com.foolish.authservice.security.CustomAccessDeniedHandler;
import com.foolish.authservice.security.CustomBasicAuthenticationEntryPoint;
import com.foolish.authservice.security.OwnUserDetailsService;
import com.foolish.authservice.security.UsernamePwdAuthenticationProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;

@Configuration
@AllArgsConstructor
public class ProjectConfigSecurity {
  private final Environment env;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(AbstractHttpConfigurer::disable);
    http.sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.csrf(AbstractHttpConfigurer::disable);
    http
            .authorizeHttpRequests(config -> config
                    .requestMatchers(
                            "/api/v1/auth/**").permitAll()
                    .anyRequest().denyAll());
    http.httpBasic(config -> config.authenticationEntryPoint(new CustomBasicAuthenticationEntryPoint()));
    http.exceptionHandling(config -> config.accessDeniedHandler(new CustomAccessDeniedHandler()));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }


  // Việc tạo ra Bean AuthenticationManager này chỉ áp dụng đối với các project yêu cầu việc login thông qua body của request.
  @Bean
  public AuthenticationManager authenticationManager(OwnUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    UsernamePwdAuthenticationProvider authenticationProvider =
            new UsernamePwdAuthenticationProvider(userDetailsService, passwordEncoder);
    ProviderManager providerManager = new ProviderManager(authenticationProvider);
    providerManager.setEraseCredentialsAfterAuthentication(false);
    return providerManager;
  }

  @Bean
  public ClientRegistration googleRegistration() {
    String googleClientId = env.getProperty("google.clientId");
    String googleClientSecret = env.getProperty("google.clientSecret");
    return CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(googleClientId).clientSecret(googleClientSecret).build();
  }

  @Bean
  ClientRegistrationRepository clientRegistrationRepository() {
    // Tạo ra các registration object. Mỗi object này đại diện cho 1 Auth Server mà chúng ta đã đăng kí.
    ClientRegistration google = googleRegistration();
    return new InMemoryClientRegistrationRepository(google);
  }


}
