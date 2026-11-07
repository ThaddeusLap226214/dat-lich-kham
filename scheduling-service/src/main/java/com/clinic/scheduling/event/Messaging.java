package com.clinic.scheduling.event;

public final class Messaging {

    public static final String PATIENT_EXCHANGE = "patient.events";
    public static final String PATIENT_EVENTS_QUEUE = "scheduling.patient-events";
    public static final String DEAD_LETTER_EXCHANGE = "patient.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "scheduling.patient-events.dlq";

    public static final String APPOINTMENT_EXCHANGE = "appointment.events";
    public static final String ROUTING_KEY_CONFIRMED = "appointment.confirmed";
    public static final String ROUTING_KEY_CANCELLED = "appointment.cancelled";

    private Messaging() {
    }
}
