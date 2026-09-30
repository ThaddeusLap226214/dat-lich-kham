package com.clinic.patient.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PatientEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PatientEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(PatientEvent event) {
        String routingKey = event.eventType() == PatientEvent.PatientEventType.PATIENT_MERGED
            ? Messaging.ROUTING_KEY_MERGED
            : Messaging.ROUTING_KEY_UNMERGED;
        rabbitTemplate.convertAndSend(Messaging.PATIENT_EXCHANGE, routingKey, event);
    }
}
