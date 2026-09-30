package com.clinic.scheduling.event;

public final class Messaging {

    public static final String PATIENT_EXCHANGE = "patient.events";
    public static final String PATIENT_EVENTS_QUEUE = "scheduling.patient-events";
    public static final String DEAD_LETTER_EXCHANGE = "patient.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "scheduling.patient-events.dlq";

    private Messaging() {
    }
}
