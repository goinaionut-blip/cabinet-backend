package ro.stoma.efactura.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "efactura")
public class EfacturaProperties {
  private boolean enabled = true;
  private String environment = "test";
  private String cif;
  private String clientId;
  private String clientSecret;
  private RedirectUri redirectUri = new RedirectUri();
  private OAuth oauth = new OAuth();
  private Api api = new Api();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getCif() {
    return cif;
  }

  public void setCif(String cif) {
    this.cif = cif;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public RedirectUri getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(RedirectUri redirectUri) {
    this.redirectUri = redirectUri;
  }

  public OAuth getOauth() {
    return oauth;
  }

  public void setOauth(OAuth oauth) {
    this.oauth = oauth;
  }

  public Api getApi() {
    return api;
  }

  public void setApi(Api api) {
    this.api = api;
  }

  public String resolveApiBase() {
    if ("prod".equalsIgnoreCase(environment)) {
      return api.getBaseProd();
    }
    return api.getBaseTest();
  }

  public static class RedirectUri {
    private String swing;
    private String backend;

    public String getSwing() {
      return swing;
    }

    public void setSwing(String swing) {
      this.swing = swing;
    }

    public String getBackend() {
      return backend;
    }

    public void setBackend(String backend) {
      this.backend = backend;
    }
  }

  public static class OAuth {
    // ANAF specific: OAuth endpoints from official gateway; update if ANAF changes URLs.
    private String authorizeUrl;
    private String tokenUrl;
    private String tokenFile = "anaf-token.json";

    public String getAuthorizeUrl() {
      return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
      this.authorizeUrl = authorizeUrl;
    }

    public String getTokenUrl() {
      return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
      this.tokenUrl = tokenUrl;
    }

    public String getTokenFile() {
      return tokenFile;
    }

    public void setTokenFile(String tokenFile) {
      this.tokenFile = tokenFile;
    }
  }

  public static class Api {
    // ANAF specific: API base URLs and paths; keep them here to update if ANAF changes endpoints.
    private String baseTest;
    private String baseProd;
    private String uploadPath;
    private String statusPath;
    private String listPath;
    private String downloadPath;
    private int listDays = 60;
    private Duration timeout = Duration.ofSeconds(90);
    private int maxRetries = 2;
    private Duration retryDelay = Duration.ofSeconds(2);

    public String getBaseTest() {
      return baseTest;
    }

    public void setBaseTest(String baseTest) {
      this.baseTest = baseTest;
    }

    public String getBaseProd() {
      return baseProd;
    }

    public void setBaseProd(String baseProd) {
      this.baseProd = baseProd;
    }

    public String getUploadPath() {
      return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
      this.uploadPath = uploadPath;
    }

    public String getStatusPath() {
      return statusPath;
    }

    public void setStatusPath(String statusPath) {
      this.statusPath = statusPath;
    }

    public String getListPath() {
      return listPath;
    }

    public void setListPath(String listPath) {
      this.listPath = listPath;
    }

    public String getDownloadPath() {
      return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
      this.downloadPath = downloadPath;
    }

    public int getListDays() {
      return listDays;
    }

    public void setListDays(int listDays) {
      this.listDays = listDays;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
      return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
      this.retryDelay = retryDelay;
    }
  }
}
