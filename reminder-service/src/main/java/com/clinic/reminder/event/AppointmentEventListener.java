package com.clinic.reminder.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final AppointmentEventHandler handler;

    public AppointmentEventListener(AppointmentEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = Messaging.APPOINTMENT_EVENTS_QUEUE)
    public void onAppointmentEvent(AppointmentEvent event) {
        log.info("Nhận sự kiện {} eventId={}, appointmentId={}",
                event.eventType(), event.eventId(), event.appointmentId());
        handler.handle(event);
    }
}
