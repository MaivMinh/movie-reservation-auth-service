package com.foolish.authservice.services;

import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.examples.lib.AuthenticateRequest;
import net.devh.boot.grpc.examples.lib.AuthenticateResponse;
import net.devh.boot.grpc.examples.lib.AuthenticateServiceGrpc;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;
import java.util.Date;

@GrpcService
@Service
@AllArgsConstructor
public class AuthenticationServiceGrpcServer extends AuthenticateServiceGrpc.AuthenticateServiceImplBase {
  private final JwtUtilsService jwtUtilsService;

  @Override
  public void doAuthenticate(AuthenticateRequest request, StreamObserver<AuthenticateResponse> streamObserver) {
    String token = request.getToken();
    try {
      Claims claims = jwtUtilsService.verifyToken(token);
      long ttl = Long.parseLong(claims.get("exp").toString()) - new Date().getTime() / 1000;
      streamObserver.onNext(AuthenticateResponse.newBuilder().setIsValid(true).setUserId(claims.get("user_id").toString()).setTtl(ttl).build());
      System.out.println("Response to gRPC Client: " + claims.get("user_id"));
    } catch (RuntimeException e) {
      streamObserver.onNext(AuthenticateResponse.newBuilder().setIsValid(false).setUserId("").setTtl(0).build());
    }
    streamObserver.onCompleted();
  }
}
