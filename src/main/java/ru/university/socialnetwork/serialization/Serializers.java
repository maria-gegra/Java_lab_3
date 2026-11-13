package ru.university.socialnetwork.serialization;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class Serializers {
    private static final List<Serializer<SocialNetworkData>> ALL =
            List.of(new JsonSocialNetworkSerializer(), new BinarySocialNetworkSerializer());

    private Serializers() {
    }

    public static List<Serializer<SocialNetworkData>> all() {
        return ALL;
    }

    public static Serializer<SocialNetworkData> byFormat(String format) {
        for (Serializer<SocialNetworkData> serializer : ALL) {
            if (serializer.format().equalsIgnoreCase(format)) return serializer;
        }
        throw new IllegalArgumentException("Неизвестный формат сериализации: " + format);
    }

    public static Serializer<SocialNetworkData> forFile(Path file) throws SerializationException {
        String name = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (Serializer<SocialNetworkData> serializer : ALL) {
            if (name.endsWith("." + serializer.fileExtension())) return serializer;
        }
        throw new SerializationException(SerializationException.Code.WRONG_FORMAT,
                "По расширению файла нельзя определить формат. Ожидается .json или .bin: " + file);
    }
}
