package com.clinic.reminder.template;

import com.clinic.reminder.log.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, ReminderType> {
}
