package ro.cabinet.backend.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import ro.cabinet.backend.notifications.dto.WhatsappWebhookResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/notifications/whatsapp")
public class WhatsappWebhookController {
  private final WhatsappWebhookService webhookService;

  public WhatsappWebhookController(WhatsappWebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping("/webhook")
  public WhatsappWebhookResponse webhook(@RequestBody(required = false) JsonNode payload) {
    return webhookService.process(payload);
  }
}
