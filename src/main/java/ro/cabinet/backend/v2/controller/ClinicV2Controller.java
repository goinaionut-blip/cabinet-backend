package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.service.ClinicV2Service;
import ro.cabinet.backend.v2.service.CurrentUserService;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/clinics")
public class ClinicV2Controller {
  private final ClinicV2Service clinicService;
  private final CurrentUserService currentUserService;

  public ClinicV2Controller(ClinicV2Service clinicService, CurrentUserService currentUserService) {
    this.clinicService = clinicService;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  public V2Dtos.ClinicResponse create(@Valid @RequestBody CreateClinicRequest request) {
    return clinicService.createClinic(currentUserService.requireCurrentUserId(), request.name(), request.slug());
  }

  @GetMapping
  public List<V2Dtos.ClinicResponse> list() {
    return clinicService.listClinics(currentUserService.requireCurrentUserId());
  }

  public record CreateClinicRequest(@NotBlank String name, String slug) {
  }
}
