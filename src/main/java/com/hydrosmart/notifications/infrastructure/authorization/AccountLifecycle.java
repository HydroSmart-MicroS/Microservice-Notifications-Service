package com.hydrosmart.notifications.infrastructure.authorization;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import com.hydrosmart.notifications.application.internal.outboundservices.AccountGuard;
import com.hydrosmart.notifications.shared.infrastructure.messaging.RpcBus;

@Component
public class AccountLifecycle implements AccountGuard {
 private final JdbcTemplate jdbc;
 private final RpcBus bus;
 private final TransactionTemplate tx;

 public AccountLifecycle(JdbcTemplate jdbc, RpcBus bus, PlatformTransactionManager manager) {
  this.jdbc = jdbc;
  this.bus = bus;
  this.tx = new TransactionTemplate(manager);
 }

 @Override
 public void read(Runnable work) {
  tx.executeWithoutResult(status -> {
   lockShared();
   work.run();
  });
 }

 @Override
 public void delivery(UUID id, Runnable work) {
  read(() -> {
   jdbc.query("SELECT pg_advisory_xact_lock(82702, ?)", rs -> null, id.hashCode());
   work.run();
  });
 }

 public <T> T write(Supplier<T> work) {
  return tx.execute(status -> {
   jdbc.query("SELECT pg_advisory_xact_lock(82701, 0)", rs -> null);
   return work.get();
  });
 }

 private void lockShared() {
  // Transaction locks coordinate both replicas and release on rollback or disconnect.
  jdbc.query("SELECT pg_advisory_xact_lock_shared(82701, 0)", rs -> null);
 }

 public boolean blocked(String user) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM privacy_receipts WHERE user_hash=encode(sha256(convert_to(?,'UTF8')),'hex') AND expires_at>now())", Boolean.class, user));
 }

 public boolean active(String user) {
  return !blocked(user) && bus.request("identity", "GET", "/internal/v1/users/" +
      java.net.URLEncoder.encode(user, java.nio.charset.StandardCharsets.UTF_8), bus.internalHeaders(), null).status() == 200;
 }

 public static class Filter extends OncePerRequestFilter {
  private final AccountLifecycle lifecycle;

  public Filter(AccountLifecycle lifecycle) {
   this.lifecycle = lifecycle;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
   if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt)) {
    chain.doFilter(request, response);
    return;
   }
   try {
    lifecycle.read(() -> {
     try {
      if (lifecycle.blocked(jwt.getToken().getSubject())) {
       response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
       return;
      }
      chain.doFilter(request, response);
     } catch (IOException | ServletException error) {
      throw new FilterFailure(error);
     }
    });
   } catch (FilterFailure error) {
    if (error.getCause() instanceof IOException io) throw io;
    throw (ServletException) error.getCause();
   }
  }
 }

 private static class FilterFailure extends RuntimeException {
  FilterFailure(Exception cause) { super(cause); }
 }
}
