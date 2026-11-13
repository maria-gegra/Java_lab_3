package ru.university.socialnetwork.bench;

import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.serialization.BinarySocialNetworkSerializer;
import ru.university.socialnetwork.serialization.JsonSocialNetworkSerializer;
import ru.university.socialnetwork.serialization.Serializer;
import ru.university.socialnetwork.serialization.SocialNetworkData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SerializationBenchmark {
    private static final int RECORDS = 10_000;
    private static final int RUNS = 5;

    private SerializationBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        SocialNetworkData data = sample(RECORDS);
        measure("JSON", new JsonSocialNetworkSerializer(), data);
        measure("BINARY", new BinarySocialNetworkSerializer(), data);
    }

    private static SocialNetworkData sample(int count) {
        List<Profile> profiles = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            profiles.add(new EditableProfile(i, "Пользователь " + i, "Город " + (i % 25), 1950 + i % 60));
        }
        List<Friendship> friendships = new ArrayList<>(count);
        for (int i = 2; i <= count; i++) {
            friendships.add(new Friendship(i - 1, i, 1 + i % 10));
        }
        return new SocialNetworkData(profiles, friendships);
    }

    private static void measure(String name, Serializer<SocialNetworkData> serializer,
                                SocialNetworkData data) throws Exception {
        Path file = Files.createTempFile("serialization-bench-", "." + serializer.fileExtension());
        try {
            serializer.save(data, file);
            serializer.load(file);
            long saveNanos = 0;
            long loadNanos = 0;
            for (int run = 0; run < RUNS; run++) {
                long started = System.nanoTime();
                serializer.save(data, file);
                saveNanos += System.nanoTime() - started;
                started = System.nanoTime();
                SocialNetworkData loaded = serializer.load(file);
                loadNanos += System.nanoTime() - started;
                if (!loaded.equals(data)) throw new IllegalStateException("Roundtrip mismatch: " + name);
            }
            System.out.printf("%s size=%d saveMs=%.3f loadMs=%.3f%n", name, Files.size(file),
                    saveNanos / 1_000_000.0 / RUNS, loadNanos / 1_000_000.0 / RUNS);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
