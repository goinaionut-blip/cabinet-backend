package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.WahaSessionStatus;

public class WahaQrResponse {
  private String sessionName;
  private String qrCode;
  private String qrImageBase64;
  private WahaSessionStatus status;
  private String message;

  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public String getQrCode() {
    return qrCode;
  }

  public void setQrCode(String qrCode) {
    this.qrCode = qrCode;
  }

  public String getQrImageBase64() {
    return qrImageBase64;
  }

  public void setQrImageBase64(String qrImageBase64) {
    this.qrImageBase64 = qrImageBase64;
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
