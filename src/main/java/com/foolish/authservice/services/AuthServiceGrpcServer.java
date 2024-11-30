package com.foolish.authservice.services;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.examples.lib.AuthRequest;
import net.devh.boot.grpc.examples.lib.AuthResponse;
import net.devh.boot.grpc.examples.lib.AuthServiceGrpc;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import javax.annotation.Nullable;


@GrpcService
@Service
@AllArgsConstructor
public class AuthServiceGrpcServer extends AuthServiceGrpc.AuthServiceImplBase {
  private final AccountService accountService;

  @Bean
  public GrpcAuthenticationReader grpcAuthenticationReader() {
    return new GrpcAuthenticationReader() {
      @Nullable
      @Override
      public Authentication readAuthentication(ServerCall<?, ?> call, Metadata headers) throws AuthenticationException {
        return null;
      }
    };
  }

  @Override
  public void doIntrospect(AuthRequest request, StreamObserver<AuthResponse> streamObserver) {
    String token = request.getToken();
    Claims claims = accountService.doIntrospect(token);
    if (claims == null) {
      streamObserver.onNext(AuthResponse.newBuilder().setActive(false).setUsername("").setRoles("").build());
    } else {
      streamObserver.onNext(AuthResponse.newBuilder().setActive(true).setUsername(claims.get("username").toString()).setRoles(claims.get("roles").toString()).build());
    }
    streamObserver.onCompleted();
  }
}