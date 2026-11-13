package com.clinic.scheduling.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public AppointmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishConfirmed(Long appointmentId, Long patientId, Long doctorId, Long roomId,
                                 LocalDateTime startTime, LocalDateTime endTime, Long performedByAccountId) {
        AppointmentEvent event = new AppointmentEvent(
            UUID.randomUUID().toString(),
            AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED,
            LocalDateTime.now(),
            appointmentId,
            patientId,
            doctorId,
            roomId,
            startTime,
            endTime,
            null,
            performedByAccountId
        );

        sendAfterCommit(Messaging.ROUTING_KEY_CONFIRMED, event);
    }

    public void publishCancelled(Long appointmentId, Long patientId, Long doctorId, Long roomId,
                                 LocalDateTime startTime, LocalDateTime endTime, String cancelReason,
                                 Long performedByAccountId) {
        AppointmentEvent event = new AppointmentEvent(
            UUID.randomUUID().toString(),
            AppointmentEvent.AppointmentEventType.APPOINTMENT_CANCELLED,
            LocalDateTime.now(),
            appointmentId,
            patientId,
            doctorId,
            roomId,
            startTime,
            endTime,
            cancelReason,
            performedByAccountId
        );

        sendAfterCommit(Messaging.ROUTING_KEY_CANCELLED, event);
    }

    // Hợp đồng A7: chỉ phát khi giao dịch đã lưu xong, để commit lỗi thì Reminder không nhận sự kiện của lịch không tồn tại
    private void sendAfterCommit(String routingKey, AppointmentEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(routingKey, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(routingKey, event);
            }
        });
    }

    private void send(String routingKey, AppointmentEvent event) {
        try {
            rabbitTemplate.convertAndSend(Messaging.APPOINTMENT_EXCHANGE, routingKey, event);
            log.info("Đã phát sự kiện {}: eventId={}, appointmentId={}", event.eventType(), event.eventId(), event.appointmentId());
        } catch (Exception e) {
            // DB đã lưu; không ném lỗi để người dùng không nhận 500 cho thao tác đã thành công
            log.error("Không phát được sự kiện {} cho lịch {}: {}", event.eventType(), event.appointmentId(), e.getMessage());
        }
    }
}
