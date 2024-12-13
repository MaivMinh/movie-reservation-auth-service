package com.foolish.authservice.services;


import com.foolish.authservice.model.Account;
import com.foolish.authservice.model.Role;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.examples.lib.IdentityRequest;
import net.devh.boot.grpc.examples.lib.IdentityResponse;
import net.devh.boot.grpc.examples.lib.IdentityServiceGrpc;
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
public class IdentityServiceGrpcServer extends IdentityServiceGrpc.IdentityServiceImplBase {
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
  public void doIdentity(IdentityRequest request, StreamObserver<IdentityResponse> streamObserver) {
    String userId = request.getUserId();
    Role role = accountService.doIntrospect(userId);
    if (role == null) {
      streamObserver.onNext(IdentityResponse.newBuilder().setActive(false).setRoles("").build());
    } else {
      streamObserver.onNext(IdentityResponse.newBuilder().setActive(true).setRoles(role.getName().toString()).build());
    }
    streamObserver.onCompleted();
  }
}