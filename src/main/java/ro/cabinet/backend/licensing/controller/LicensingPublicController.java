package ro.cabinet.backend.licensing.controller;

import ro.cabinet.backend.licensing.dto.ActivateRequest;
import ro.cabinet.backend.licensing.dto.EntitlementResponse;
import ro.cabinet.backend.licensing.dto.TouchRequest;
import ro.cabinet.backend.licensing.service.LicensingService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licensing/v1")
public class LicensingPublicController {
  private final LicensingService licensingService;

  public LicensingPublicController(LicensingService licensingService) {
    this.licensingService = licensingService;
  }

  @PostMapping("/installs/touch")
  public EntitlementResponse touch(@Valid @RequestBody TouchRequest request) {
    return licensingService.touch(request);
  }

  @PostMapping("/licenses/activate")
  public EntitlementResponse activate(@Valid @RequestBody ActivateRequest request) {
    return licensingService.activate(request);
  }
}
