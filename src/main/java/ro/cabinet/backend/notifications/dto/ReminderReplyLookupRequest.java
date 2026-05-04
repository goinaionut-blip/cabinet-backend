package ro.cabinet.backend.notifications.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ReminderReplyLookupRequest {
  @NotNull
  private UUID clinicId;

  @Valid
  private List<ReminderReplyLookupItem> reminders = new ArrayList<>();

  public UUID getClinicId() {
    return clinicId;
  }

  public void setClinicId(UUID clinicId) {
    this.clinicId = clinicId;
  }

  public List<ReminderReplyLookupItem> getReminders() {
    return reminders;
  }

  public void setReminders(List<ReminderReplyLookupItem> reminders) {
    this.reminders = reminders == null ? new ArrayList<>() : reminders;
  }
}
