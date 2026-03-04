package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.V2AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
public class V2AuthController {
  private final V2AuthService authService;
  private final CurrentUserService currentUserService;

  public V2AuthController(V2AuthService authService, CurrentUserService currentUserService) {
    this.authService = authService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/login")
  public V2Dtos.AuthLoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.email(), request.password());
  }

  @GetMapping("/me")
  public V2Dtos.AuthMeResponse me() {
    return authService.me(currentUserService.requireCurrentUserId());
  }

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {
  }
}
