package com.ufh.ufhresidencemaintenance.model;

public enum ComplaintCategory {
    ELECTRICAL(5),
    PLUMBING(4),
    STRUCTURAL(3),
    OTHER(2),
    FURNITURE(1);

    private final int severityWeight;

    ComplaintCategory(int severityWeight) {
        this.severityWeight = severityWeight;
    }

    public int getSeverityWeight() {
        return severityWeight;
    }
}
