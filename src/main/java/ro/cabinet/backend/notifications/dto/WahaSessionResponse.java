package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.WahaSessionStatus;

public class WahaSessionResponse {
  private String sessionName;
  private WahaSessionStatus status;
  private String message;

  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public WahaSessionStatus getStatus() {
    return status;
  }

  public void setStatus(WahaSessionStatus status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
