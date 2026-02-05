package ro.cabinet.backend.licensing.controller;

import ro.cabinet.backend.licensing.dto.AdminActivationResponse;
import ro.cabinet.backend.licensing.dto.AdminCreateLicenseRequest;
import ro.cabinet.backend.licensing.dto.AdminCreateLicenseResponse;
import ro.cabinet.backend.licensing.dto.AdminCreateProductRequest;
import ro.cabinet.backend.licensing.dto.AdminLicenseResponse;
import ro.cabinet.backend.licensing.dto.AdminProductResponse;
import ro.cabinet.backend.licensing.service.LicensingAdminGuard;
import ro.cabinet.backend.licensing.service.LicensingAdminService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licensing/v1/admin")
public class LicensingAdminController {
  private final LicensingAdminService adminService;
  private final LicensingAdminGuard adminGuard;

  public LicensingAdminController(LicensingAdminService adminService, LicensingAdminGuard adminGuard) {
    this.adminService = adminService;
    this.adminGuard = adminGuard;
  }

  @PostMapping("/licenses")
  public AdminCreateLicenseResponse createLicense(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken,
      @Valid @RequestBody AdminCreateLicenseRequest request) {
    adminGuard.requireAuthorized(adminToken);
    return adminService.createLicense(request);
  }

  @PostMapping("/licenses/{licenseId}/revoke")
  public void revokeLicense(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken,
      @PathVariable("licenseId") UUID licenseId) {
    adminGuard.requireAuthorized(adminToken);
    adminService.revokeLicense(licenseId);
  }

  @GetMapping("/licenses")
  public List<AdminLicenseResponse> listLicenses(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken,
      @RequestParam(value = "productCode", required = false) String productCode) {
    adminGuard.requireAuthorized(adminToken);
    return adminService.listLicenses(productCode);
  }

  @GetMapping("/activations")
  public List<AdminActivationResponse> listActivations(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken,
      @RequestParam(value = "productCode") String productCode,
      @RequestParam(value = "installId", required = false) UUID installId) {
    adminGuard.requireAuthorized(adminToken);
    return adminService.listActivations(productCode, installId);
  }

  @GetMapping("/products")
  public List<AdminProductResponse> listProducts(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken) {
    adminGuard.requireAuthorized(adminToken);
    return adminService.listProducts();
  }

  @PostMapping("/products")
  public AdminProductResponse createProduct(
      @RequestHeader(value = "X-Licensing-Admin-Token", required = false) String adminToken,
      @Valid @RequestBody AdminCreateProductRequest request) {
    adminGuard.requireAuthorized(adminToken);
    return adminService.createProduct(request);
  }
}
