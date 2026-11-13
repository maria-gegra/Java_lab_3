package ru.university.socialnetwork.serialization;

import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.util.HashSet;
import java.util.Set;

final class NetworkDataValidator {
    private NetworkDataValidator() {
    }

    static SocialNetworkData validate(SocialNetworkData data) throws SerializationException {
        if (data == null) {
            throw new SerializationException(SerializationException.Code.VALIDATION_FAILED,
                    "Файл не содержит данных социальной сети");
        }
        if (data.schemaVersion() != SocialNetworkData.CURRENT_SCHEMA_VERSION) {
            throw new SerializationException(SerializationException.Code.UNSUPPORTED_VERSION,
                    "Неподдерживаемая версия формата: " + data.schemaVersion()
                            + ". Поддерживается версия " + SocialNetworkData.CURRENT_SCHEMA_VERSION);
        }
        if (data.profiles() == null || data.friendships() == null) {
            throw new SerializationException(SerializationException.Code.VALIDATION_FAILED,
                    "В файле отсутствуют списки профилей или дружб");
        }
        Set<Integer> ids = new HashSet<>();
        for (Profile profile : data.profiles()) {
            if (profile == null) {
                throw failed("Список профилей содержит пустую запись");
            }
            if (!profile.validate().isEmpty()) {
                throw failed("Некорректный профиль " + profile.getId() + ": "
                        + String.join("; ", profile.validate()));
            }
            if (!ids.add(profile.getId())) {
                throw failed("Дублирующийся ID профиля: " + profile.getId());
            }
        }
        Set<Long> pairs = new HashSet<>();
        for (Friendship friendship : data.friendships()) {
            if (friendship == null) {
                throw failed("Список дружб содержит пустую запись");
            }
            if (!ids.contains(friendship.getFirstId()) || !ids.contains(friendship.getSecondId())) {
                throw failed("Дружба " + friendship.getFirstId() + " — " + friendship.getSecondId()
                        + " ссылается на отсутствующий профиль");
            }
            int low = Math.min(friendship.getFirstId(), friendship.getSecondId());
            int high = Math.max(friendship.getFirstId(), friendship.getSecondId());
            long key = ((long) low << 32) | (high & 0xffffffffL);
            if (!pairs.add(key)) {
                throw failed("Дублирующаяся дружба: " + low + " — " + high);
            }
        }
        return data;
    }

    private static SerializationException failed(String message) {
        return new SerializationException(SerializationException.Code.VALIDATION_FAILED, message);
    }
}
