package com.clinic.reminder.event;

public final class Messaging {

    public static final String APPOINTMENT_EXCHANGE = "appointment.events";
    public static final String APPOINTMENT_EVENTS_QUEUE = "reminder.appointment-events";
    public static final String DEAD_LETTER_EXCHANGE = "appointment.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "reminder.appointment-events.dlq";

    public static final String ROUTING_KEY_CONFIRMED = "appointment.confirmed";
    public static final String ROUTING_KEY_CANCELLED = "appointment.cancelled";

    private Messaging() {
    }
}
