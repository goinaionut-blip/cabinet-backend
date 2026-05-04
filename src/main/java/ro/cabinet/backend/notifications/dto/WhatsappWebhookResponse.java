package ro.cabinet.backend.notifications.dto;

public class WhatsappWebhookResponse {
  private String status;

  public WhatsappWebhookResponse() {
  }

  public WhatsappWebhookResponse(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
