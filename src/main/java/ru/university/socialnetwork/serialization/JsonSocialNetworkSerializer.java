package ru.university.socialnetwork.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JsonSocialNetworkSerializer implements Serializer<SocialNetworkData> {
    static final String FORMAT = "json";
    static final String EXTENSION = "json";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public String fileExtension() {
        return EXTENSION;
    }

    @Override
    public void save(SocialNetworkData data, Path file) throws IOException {
        Objects.requireNonNull(data, "Данные не заданы");
        Objects.requireNonNull(file, "Файл не задан");
        NetworkDataValidator.validate(data);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".social-network-", ".tmp");
        try {
            mapper.writeValue(temporary.toFile(), JsonData.from(data));
            move(temporary, file);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public SocialNetworkData load(Path file) throws IOException {
        Objects.requireNonNull(file, "Файл не задан");
        if (Files.size(file) == 0) {
            throw new SerializationException(SerializationException.Code.EMPTY_FILE, "Файл пуст: " + file);
        }
        JsonData json;
        try {
            json = mapper.readValue(file.toFile(), JsonData.class);
        } catch (JsonProcessingException e) {
            throw new SerializationException(SerializationException.Code.CORRUPTED_FILE,
                    "JSON-файл повреждён: " + e.getOriginalMessage(), e);
        }
        if (json == null) {
            throw new SerializationException(SerializationException.Code.EMPTY_FILE,
                    "JSON-файл не содержит данных");
        }
        return NetworkDataValidator.validate(toData(json));
    }

    private static SocialNetworkData toData(JsonData json) throws SerializationException {
        if (json.profiles() == null || json.friendships() == null) {
            throw new SerializationException(SerializationException.Code.VALIDATION_FAILED,
                    "В JSON отсутствуют поля profiles или friendships");
        }
        try {
            List<Profile> profiles = new ArrayList<>(json.profiles().size());
            for (JsonProfile item : json.profiles()) {
                if (item == null || item.type() == null) {
                    throw new IllegalArgumentException("Не указан тип профиля");
                }
                Profile profile = switch (item.type()) {
                    case PROFILE -> new EditableProfile(item.id(), item.name(), item.city(), item.birthYear());
                    case DELETED -> new DeletedProfile(item.id(), item.name(), item.city(), item.birthYear(),
                            item.reason() == null ? "" : item.reason());
                    case COMMUNITY -> new Community(item.id(), item.name(), item.city(), item.birthYear(),
                            item.description(), item.administratorId() == null ? 0 : item.administratorId());
                };
                profiles.add(profile);
            }
            List<Friendship> friendships = new ArrayList<>(json.friendships().size());
            for (JsonFriendship item : json.friendships()) {
                if (item == null) throw new IllegalArgumentException("Пустая запись дружбы");
                friendships.add(new Friendship(item.firstId(), item.secondId(), item.strength()));
            }
            return new SocialNetworkData(json.schemaVersion(), profiles, friendships);
        } catch (IllegalArgumentException e) {
            throw new SerializationException(SerializationException.Code.VALIDATION_FAILED,
                    "Некорректные данные в JSON: " + e.getMessage(), e);
        }
    }

    record JsonProfile(@JsonProperty("type") ProfileType type,
                       @JsonProperty("id") int id,
                       @JsonProperty("name") String name,
                       @JsonProperty("city") String city,
                       @JsonProperty("birthYear") int birthYear,
                       @JsonProperty("description") String description,
                       @JsonProperty("administratorId") Integer administratorId,
                       @JsonProperty("reason") String reason) {
        static JsonProfile from(Profile profile) {
            if (profile instanceof Community community) {
                return new JsonProfile(ProfileType.COMMUNITY, profile.getId(), profile.getName(),
                        profile.getCity(), profile.getBirthYear(), community.getDescription(),
                        community.getAdministratorId(), null);
            }
            if (profile instanceof DeletedProfile deleted) {
                return new JsonProfile(ProfileType.DELETED, profile.getId(), profile.getName(),
                        profile.getCity(), profile.getBirthYear(), null, null, deleted.getReason());
            }
            return new JsonProfile(ProfileType.PROFILE, profile.getId(), profile.getName(),
                    profile.getCity(), profile.getBirthYear(), null, null, null);
        }
    }

    record JsonFriendship(@JsonProperty("firstId") int firstId,
                          @JsonProperty("secondId") int secondId,
                          @JsonProperty("strength") int strength) {
        static JsonFriendship from(Friendship friendship) {
            return new JsonFriendship(friendship.getFirstId(), friendship.getSecondId(), friendship.getStrength());
        }
    }

    record JsonData(@JsonProperty("schemaVersion") int schemaVersion,
                    @JsonProperty("profiles") List<JsonProfile> profiles,
                    @JsonProperty("friendships") List<JsonFriendship> friendships) {
        static JsonData from(SocialNetworkData data) {
            return new JsonData(data.schemaVersion(),
                    data.profiles().stream().map(JsonProfile::from).toList(),
                    data.friendships().stream().map(JsonFriendship::from).toList());
        }
    }

    private static void move(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
