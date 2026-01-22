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
}
