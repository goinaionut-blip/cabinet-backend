package ro.cabinet.backend.notifications.client;

import ro.cabinet.backend.notifications.WahaSessionStatus;

public class WahaSessionInfo {
  private final String sessionName;
  private final WahaSessionStatus status;
  private final String message;

  public WahaSessionInfo(String sessionName, WahaSessionStatus status, String message) {
    this.sessionName = sessionName;
    this.status = status;
    this.message = message;
  }

  public String getSessionName() {
    return sessionName;
  }

  public WahaSessionStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
