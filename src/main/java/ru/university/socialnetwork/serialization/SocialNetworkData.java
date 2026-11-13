package ru.university.socialnetwork.serialization;

import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public final class SocialNetworkData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final List<Profile> profiles;
    private final List<Friendship> friendships;

    public SocialNetworkData(List<Profile> profiles, List<Friendship> friendships) {
        this(CURRENT_SCHEMA_VERSION, profiles, friendships);
    }

    public SocialNetworkData(int schemaVersion, List<Profile> profiles, List<Friendship> friendships) {
        this.schemaVersion = schemaVersion;
        this.profiles = profiles == null ? null : List.copyOf(profiles);
        this.friendships = friendships == null ? null : List.copyOf(friendships);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public List<Profile> profiles() {
        return profiles;
    }

    public List<Friendship> friendships() {
        return friendships;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SocialNetworkData that)) return false;
        return schemaVersion == that.schemaVersion
                && Objects.equals(profiles, that.profiles)
                && Objects.equals(friendships, that.friendships);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, profiles, friendships);
    }

    @Override
    public String toString() {
        return "SocialNetworkData[schemaVersion=" + schemaVersion
                + ", profiles=" + (profiles == null ? null : profiles.size())
                + ", friendships=" + (friendships == null ? null : friendships.size()) + "]";
    }
}
