package ru.university.socialnetwork.serialization;

import java.io.IOException;
import java.nio.file.Path;

public interface Serializer<T> {
    String format();

    String fileExtension();

    void save(T data, Path file) throws IOException;

    T load(Path file) throws IOException;
}
