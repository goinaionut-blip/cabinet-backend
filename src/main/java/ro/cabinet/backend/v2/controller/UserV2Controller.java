package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.UserManagementV2Service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/users")
public class UserV2Controller {
  private final UserManagementV2Service userManagementV2Service;
  private final CurrentUserService currentUserService;

  public UserV2Controller(UserManagementV2Service userManagementV2Service, CurrentUserService currentUserService) {
    this.userManagementV2Service = userManagementV2Service;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  public V2Dtos.UserSummary create(@Valid @RequestBody CreateUserRequest request) {
    return userManagementV2Service.createUser(
        currentUserService.requireCurrentUserId(),
        request.email(),
        request.password(),
        request.displayName(),
        request.active());
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> delete(@PathVariable @NotNull UUID userId) {
    userManagementV2Service.deleteUser(currentUserService.requireCurrentUserId(), userId);
    return ResponseEntity.noContent().build();
  }

  public record CreateUserRequest(@NotBlank String email,
                                  @NotBlank String password,
                                  String displayName,
                                  Boolean active) {
  }
}
