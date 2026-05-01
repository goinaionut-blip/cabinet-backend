package ro.cabinet.backend.notifications.client;

public class ProviderSendResult {
  private final boolean success;
  private final String providerMessageId;
  private final String errorMessage;

  private ProviderSendResult(boolean success, String providerMessageId, String errorMessage) {
    this.success = success;
    this.providerMessageId = providerMessageId;
    this.errorMessage = errorMessage;
  }

  public static ProviderSendResult success(String providerMessageId) {
    return new ProviderSendResult(true, providerMessageId, null);
  }

  public static ProviderSendResult failure(String errorMessage) {
    return new ProviderSendResult(false, null, errorMessage);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
