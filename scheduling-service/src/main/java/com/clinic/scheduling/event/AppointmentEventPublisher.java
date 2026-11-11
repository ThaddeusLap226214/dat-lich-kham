package com.clinic.scheduling.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

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

        log.info("Phát sự kiện APPOINTMENT_CONFIRMED: eventId={}, appointmentId={}", event.eventId(), appointmentId);
        rabbitTemplate.convertAndSend(Messaging.APPOINTMENT_EXCHANGE, Messaging.ROUTING_KEY_CONFIRMED, event);
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

        log.info("Phát sự kiện APPOINTMENT_CANCELLED: eventId={}, appointmentId={}", event.eventId(), appointmentId);
        rabbitTemplate.convertAndSend(Messaging.APPOINTMENT_EXCHANGE, Messaging.ROUTING_KEY_CANCELLED, event);
    }
}
