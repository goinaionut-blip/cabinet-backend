package ro.cabinet.backend.sign;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sign")
public class SignProperties {
  private String baseUrl;
  private String apiKey;
  private int tokenTtlMinutes = 15;
  private String publicBaseUrl;
  private int cleanupHours = 6;
  private Storage storage = new Storage();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public int getTokenTtlMinutes() {
    return tokenTtlMinutes;
  }

  public void setTokenTtlMinutes(int tokenTtlMinutes) {
    this.tokenTtlMinutes = tokenTtlMinutes;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public int getCleanupHours() {
    return cleanupHours;
  }

  public void setCleanupHours(int cleanupHours) {
    this.cleanupHours = cleanupHours;
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public static class Storage {
    private String baseDir;
    private String incomingDir;
    private String signedDir;

    public String getBaseDir() {
      return baseDir;
    }

    public void setBaseDir(String baseDir) {
      this.baseDir = baseDir;
    }

    public String getIncomingDir() {
      return incomingDir;
    }

    public void setIncomingDir(String incomingDir) {
      this.incomingDir = incomingDir;
    }

    public String getSignedDir() {
      return signedDir;
    }

    public void setSignedDir(String signedDir) {
      this.signedDir = signedDir;
    }
  }
}
