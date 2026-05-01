package ro.cabinet.backend.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifications")
public class NotificationsProperties {
  private final WahaProperties waha = new WahaProperties();
  private final SmsProperties sms = new SmsProperties();

  public WahaProperties getWaha() {
    return waha;
  }

  public SmsProperties getSms() {
    return sms;
  }

  public static class WahaProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String defaultSession = "default";
    private int timeoutSeconds = 10;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

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

    public String getDefaultSession() {
      return defaultSession;
    }

    public void setDefaultSession(String defaultSession) {
      this.defaultSession = defaultSession;
    }

    public int getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }

  public static class SmsProperties {
    private final MessaggioProperties messaggio = new MessaggioProperties();

    public MessaggioProperties getMessaggio() {
      return messaggio;
    }
  }

  public static class MessaggioProperties {
    private boolean enabled;
    private String baseUrl = "https://msg.messaggio.com/api/v1";
    private String login;
    private String senderId;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getLogin() {
      return login;
    }

    public void setLogin(String login) {
      this.login = login;
    }

    public String getSenderId() {
      return senderId;
    }

    public void setSenderId(String senderId) {
      this.senderId = senderId;
    }
  }
}
