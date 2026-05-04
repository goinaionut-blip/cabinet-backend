package ro.cabinet.backend.notifications;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhatsappReplyClassifierTest {
  private final WhatsappReplyClassifier classifier = new WhatsappReplyClassifier();

  @Test
  void confirmsAppointmentWhenReplyIsPositive() {
    assertEquals(NotificationReplyStatus.CONFIRMED, classifier.classify("DA, ajung", true));
  }

  @Test
  void marksAppointmentNegativeReplyForReview() {
    assertEquals(NotificationReplyStatus.NEEDS_REVIEW, classifier.classify("nu pot", true));
  }

  @Test
  void marksRecallPositiveReplyForReview() {
    assertEquals(NotificationReplyStatus.NEEDS_REVIEW, classifier.classify("confirm", false));
  }

  @Test
  void declinesRecallWhenReplyIsNegative() {
    assertEquals(NotificationReplyStatus.DECLINED, classifier.classify("altă dată", false));
  }
}
