package ru.university.socialnetwork.model;

import java.util.ArrayList;
import java.util.List;

public final class DeletedProfile extends Profile {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private final String reason;

    public DeletedProfile(int id, String name, String city, int birthYear, String reason) {
        super(id, name, city, birthYear);
        this.reason = reason == null ? "" : reason;
    }

    public String getReason() { return reason; }

    @Override
    public List<ValidationIssue> validationIssues() {
        List<ValidationIssue> errors = new ArrayList<>(super.validationIssues());
        checkText(errors, "reason", "Причина удаления", reason, false);
        return List.copyOf(errors);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && java.util.Objects.equals(reason, ((DeletedProfile) other).reason);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), reason);
    }

    @Override
    public String toString() { return super.toString() + ", удалён: " + reason; }
}
