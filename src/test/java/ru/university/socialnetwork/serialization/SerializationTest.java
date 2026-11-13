package ru.university.socialnetwork.serialization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest {
    @TempDir
    Path tempDir;

    private static SocialNetworkData sample() {
        List<Profile> profiles = List.of(
                new EditableProfile(1, "Анна Соколова", "Новосибирск", 1998),
                new EditableProfile(2, "Борис Волков", "Омск", 1995),
                new Community(3, "Клуб Java", "Новосибирск", 2015, "Сообщество любителей Java", 1),
                new DeletedProfile(4, "Удалённый пользователь", "Томск", 1990, "Нарушение правил"));
        return new SocialNetworkData(profiles, List.of(new Friendship(1, 2, 7)));
    }

    private Path resource(String name) throws Exception {
        Path target = tempDir.resolve(name);
        try (InputStream in = getClass().getResourceAsStream("/" + name)) {
            if (in == null) throw new IllegalStateException("Нет тестового ресурса " + name);
            Files.copy(in, target);
        }
        return target;
    }

    @Test
    void jsonRoundtripPreservesTypesAndFields() throws Exception {
        Serializer<SocialNetworkData> serializer = new JsonSocialNetworkSerializer();
        Path file = tempDir.resolve("data.json");
        serializer.save(sample(), file);
        assertEquals(sample(), serializer.load(file));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"schemaVersion\" : 1"));
    }

    @Test
    void binaryRoundtripPreservesTypesAndFields() throws Exception {
        Serializer<SocialNetworkData> serializer = new BinarySocialNetworkSerializer();
        Path file = tempDir.resolve("data.bin");
        serializer.save(sample(), file);
        assertEquals(sample(), serializer.load(file));
    }

    @Test
    void jsonLoadsPreparedFile() throws Exception {
        SocialNetworkData loaded = new JsonSocialNetworkSerializer().load(resource("social-network.json"));
        assertEquals(sample(), loaded);
    }

    @Test
    void binaryLoadsPreparedFile() throws Exception {
        SocialNetworkData loaded = new BinarySocialNetworkSerializer().load(resource("social-network.bin"));
        assertEquals(sample(), loaded);
    }

    @Test
    void bothFormatsLoadSamePreparedData() throws Exception {
        SocialNetworkData fromJson = new JsonSocialNetworkSerializer().load(resource("social-network.json"));
        SocialNetworkData fromBinary = new BinarySocialNetworkSerializer().load(resource("social-network.bin"));
        assertEquals(fromJson, fromBinary);
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        Path file = Files.createFile(tempDir.resolve("empty.json"));
        SerializationException error = assertThrows(SerializationException.class,
                () -> new JsonSocialNetworkSerializer().load(file));
        assertEquals(SerializationException.Code.EMPTY_FILE, error.code());
    }

    @Test
    void corruptedJsonIsRejected() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ \"schemaVersion\": 1, \"profiles\": [", StandardCharsets.UTF_8);
        SerializationException error = assertThrows(SerializationException.class,
                () -> new JsonSocialNetworkSerializer().load(file));
        assertEquals(SerializationException.Code.CORRUPTED_FILE, error.code());
    }

    @Test
    void corruptedBinaryIsRejected() throws Exception {
        Path file = tempDir.resolve("broken.bin");
        Files.write(file, new byte[]{1, 2, 3, 4, 5});
        assertThrows(SerializationException.class, () -> new BinarySocialNetworkSerializer().load(file));
    }

    @Test
    void jsonWithUnsupportedVersionIsRejected() throws Exception {
        String text = Files.readString(resource("social-network.json"), StandardCharsets.UTF_8)
                .replace("\"schemaVersion\" : 1", "\"schemaVersion\" : 99");
        Path file = tempDir.resolve("future.json");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        SerializationException error = assertThrows(SerializationException.class,
                () -> new JsonSocialNetworkSerializer().load(file));
        assertEquals(SerializationException.Code.UNSUPPORTED_VERSION, error.code());
    }

    @Test
    void binaryFileFedToJsonSerializerIsRejected() throws Exception {
        Path file = tempDir.resolve("data.bin");
        new BinarySocialNetworkSerializer().save(sample(), file);
        assertThrows(SerializationException.class, () -> new JsonSocialNetworkSerializer().load(file));
    }

    @Test
    void jsonFileFedToBinarySerializerIsRejected() throws Exception {
        Path file = tempDir.resolve("data.json");
        new JsonSocialNetworkSerializer().save(sample(), file);
        assertThrows(SerializationException.class, () -> new BinarySocialNetworkSerializer().load(file));
    }

    @Test
    void friendshipToMissingProfileIsRejected() throws Exception {
        SocialNetworkData bad = new SocialNetworkData(
                List.of(new EditableProfile(1, "Анна Соколова", "Новосибирск", 1998)),
                List.of(new Friendship(1, 99, 5)));
        Path file = tempDir.resolve("bad.json");
        SerializationException error = assertThrows(SerializationException.class,
                () -> new JsonSocialNetworkSerializer().save(bad, file));
        assertEquals(SerializationException.Code.VALIDATION_FAILED, error.code());
    }

    @Test
    void duplicateProfileIdInJsonIsRejected() throws Exception {
        String text = Files.readString(resource("social-network.json"), StandardCharsets.UTF_8)
                .replace("\"id\" : 2", "\"id\" : 1");
        Path file = tempDir.resolve("duplicate.json");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        SerializationException error = assertThrows(SerializationException.class,
                () -> new JsonSocialNetworkSerializer().load(file));
        assertEquals(SerializationException.Code.VALIDATION_FAILED, error.code());
    }

    @Test
    void loadingMissingFileThrowsIoException() {
        assertThrows(java.io.IOException.class,
                () -> new JsonSocialNetworkSerializer().load(tempDir.resolve("absent.json")));
    }

    @Test
    void serializerIsChosenByExtension() throws Exception {
        assertEquals("json", Serializers.forFile(Path.of("a.json")).format());
        assertEquals("binary", Serializers.forFile(Path.of("a.bin")).format());
        assertThrows(SerializationException.class, () -> Serializers.forFile(Path.of("a.txt")));
    }
}
