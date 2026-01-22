package ro.stoma.efactura.web;

import ro.stoma.efactura.oauth.AnafOAuthService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/efactura/oauth")
public class AnafOAuthController {
  private final AnafOAuthService oauthService;

  public AnafOAuthController(AnafOAuthService oauthService) {
    this.oauthService = oauthService;
  }

  @GetMapping(value = "/login-url", produces = MediaType.TEXT_PLAIN_VALUE)
  public String loginUrl() {
    return oauthService.buildLoginUrl();
  }

  @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
  public String callback(@RequestParam("code") String code) {
    oauthService.exchangeCodeForToken(code);
    return "Autentificare reușită";
  }

  @GetMapping(value = "/has-token", produces = MediaType.TEXT_PLAIN_VALUE)
  public String hasToken() {
    return Boolean.toString(oauthService.hasToken());
  }

  @GetMapping(value = "/ping-anaf", produces = MediaType.TEXT_PLAIN_VALUE)
  public String pingAnaf() {
    return oauthService.pingAuthorizeEndpoint();
  }

  @GetMapping(value = "/ping-anaf-token", produces = MediaType.TEXT_PLAIN_VALUE)
  public org.springframework.http.ResponseEntity<String> pingAnafToken() {
    String result = oauthService.pingTokenEndpoint();
    if (result.startsWith("OK ")) {
      return org.springframework.http.ResponseEntity.ok(result);
    }
    return org.springframework.http.ResponseEntity.status(502).body(result);
  }

  @GetMapping(value = "/resolve-anaf", produces = MediaType.TEXT_PLAIN_VALUE)
  public String resolveAnaf() {
    return oauthService.resolveAnafHost();
  }
}
