package com.clinic.reminder.template;

import com.clinic.reminder.log.ReminderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReminderType type;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "updated_by_account_id")
    private Long updatedByAccountId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MessageTemplate(ReminderType type, String subject, String body) {
        this.type = type;
        this.subject = subject;
        this.body = body;
    }
}
