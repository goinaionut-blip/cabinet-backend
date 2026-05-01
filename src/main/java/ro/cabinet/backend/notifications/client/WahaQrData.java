package ro.cabinet.backend.notifications.client;

import ro.cabinet.backend.notifications.WahaSessionStatus;

public class WahaQrData {
  private final String sessionName;
  private final String qrCode;
  private final String qrImageBase64;
  private final WahaSessionStatus status;
  private final String message;

  public WahaQrData(String sessionName, String qrCode, String qrImageBase64,
                    WahaSessionStatus status, String message) {
    this.sessionName = sessionName;
    this.qrCode = qrCode;
    this.qrImageBase64 = qrImageBase64;
    this.status = status;
    this.message = message;
  }

  public String getSessionName() {
    return sessionName;
  }

  public String getQrCode() {
    return qrCode;
  }

  public String getQrImageBase64() {
    return qrImageBase64;
  }

  public WahaSessionStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
