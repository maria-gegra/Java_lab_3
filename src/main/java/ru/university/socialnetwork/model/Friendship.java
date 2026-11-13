package ru.university.socialnetwork.model;

public final class Friendship implements java.io.Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private final int firstId;
    private final int secondId;
    private int strength;

    public Friendship(int firstId, int secondId, int strength) {
        if (firstId < 1 || secondId < 1) {
            throw new IllegalArgumentException("ID участников дружбы должны быть положительными");
        }
        if (firstId == secondId) {
            throw new IllegalArgumentException("Нельзя добавить дружбу с самим собой");
        }
        this.firstId = firstId;
        this.secondId = secondId;
        setStrength(strength);
    }

    public int getFirstId() { return firstId; }
    public int getSecondId() { return secondId; }
    public int getStrength() { return strength; }

    public void setStrength(int strength) {
        if (strength < 1) {
            throw new IllegalArgumentException("Сила связи должна быть положительной");
        }
        this.strength = strength;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Friendship that)) return false;
        return firstId == that.firstId && secondId == that.secondId && strength == that.strength;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(firstId, secondId, strength);
    }

    @Override
    public String toString() {
        return firstId + " — " + secondId + " (" + strength + ")";
    }

    public int getOtherId(int id) {
        if (id == firstId) return secondId;
        if (id == secondId) return firstId;
        throw new IllegalArgumentException("ID " + id + " не входит в эту связь");
    }
}
