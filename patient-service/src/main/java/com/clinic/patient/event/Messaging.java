package com.clinic.patient.event;

public final class Messaging {

    public static final String PATIENT_EXCHANGE = "patient.events";
    public static final String ROUTING_KEY_MERGED = "patient.merged";
    public static final String ROUTING_KEY_UNMERGED = "patient.unmerged";

    private Messaging() {
    }
}
