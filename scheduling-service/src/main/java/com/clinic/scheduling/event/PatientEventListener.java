package com.clinic.scheduling.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientEventListener {

    private static final Logger log = LoggerFactory.getLogger(PatientEventListener.class);

    private final PatientEventHandler handler;

    public PatientEventListener(PatientEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = Messaging.PATIENT_EVENTS_QUEUE)
    public void onPatientEvent(PatientEvent event) {
        log.info("Nhận sự kiện {} eventId={}", event.eventType(), event.eventId());
        handler.handle(event);
    }
}
