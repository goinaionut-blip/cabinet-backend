package ro.stoma.efactura.client;

public class AnafApiException extends RuntimeException {
  private final int status;

  public AnafApiException(String operation, int status, String body) {
    super("ANAF API error during " + operation + " (status " + status + ")" + formatBody(body));
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  private static String formatBody(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    return ": " + body;
  }
}
