package ru.university.socialnetwork.serialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class BinarySocialNetworkSerializer implements Serializer<SocialNetworkData> {
    static final int MAGIC = 0x534E4231;
    static final String FORMAT = "binary";
    static final String EXTENSION = "bin";

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
            try (OutputStream raw = new BufferedOutputStream(Files.newOutputStream(temporary));
                 ObjectOutputStream out = new ObjectOutputStream(raw)) {
                out.writeInt(MAGIC);
                out.writeInt(SocialNetworkData.CURRENT_SCHEMA_VERSION);
                out.writeObject(data);
            }
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
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(file));
             ObjectInputStream in = new ObjectInputStream(raw)) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new SerializationException(SerializationException.Code.WRONG_FORMAT,
                        "Файл не является бинарным файлом социальной сети (неверная сигнатура)");
            }
            int version = in.readInt();
            if (version != SocialNetworkData.CURRENT_SCHEMA_VERSION) {
                throw new SerializationException(SerializationException.Code.UNSUPPORTED_VERSION,
                        "Неподдерживаемая версия бинарного формата: " + version
                                + ". Поддерживается версия " + SocialNetworkData.CURRENT_SCHEMA_VERSION);
            }
            Object object = in.readObject();
            if (!(object instanceof SocialNetworkData data)) {
                throw new SerializationException(SerializationException.Code.WRONG_FORMAT,
                        "В бинарном файле записан объект другого типа");
            }
            return NetworkDataValidator.validate(data);
        } catch (SerializationException e) {
            throw e;
        } catch (EOFException | StreamCorruptedException | InvalidClassException
                 | ClassNotFoundException | ClassCastException e) {
            throw new SerializationException(SerializationException.Code.CORRUPTED_FILE,
                    "Бинарный файл повреждён или обрезан: " + e.getMessage(), e);
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
