package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.ProfileRepository;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainWindowSerializationTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void startFx() throws Exception {
        FxTestSupport.initialize();
    }

    private static Object field(MainWindow app, String name) throws Exception {
        Field field = MainWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(app);
    }

    @Test
    void exportThenImportRestoresTableThroughBackgroundTasks() throws Exception {
        Path file = directory.resolve("export.json");
        MainWindow app = FxTestSupport.run(() -> {
            MainWindow window = new MainWindow();
            window.start(new Stage());
            ProfileRepository repository = (ProfileRepository) field(window, "repository");
            repository.replaceAll(List.of(
                    new EditableProfile(1, "Анна", "Москва", 2000),
                    new EditableProfile(2, "Борис", "Омск", 1999)));
            repository.addFriendship(new Friendship(1, 2, 5));
            return window;
        });
        try {
            assertNotNull(FxTestSupport.run(() -> ((Stage) field(app, "stage")).getScene().lookup("#exportButton")));
            assertNotNull(FxTestSupport.run(() -> ((Stage) field(app, "stage")).getScene().lookup("#importButton")));
            awaitOperation(app, () -> app.exportFile(file));
            FxTestSupport.run(() -> {
                ((ProfileRepository) field(app, "repository")).clear();
                return null;
            });
            awaitOperation(app, () -> app.importFile(file));
            assertEquals(2, FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).size()));
            assertEquals(1, FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).friendshipCount()));
            @SuppressWarnings("unchecked")
            TableView<Profile> table = FxTestSupport.run(() -> (TableView<Profile>) field(app, "table"));
            assertEquals(2, FxTestSupport.run(table::getItems).size());
        } finally {
            FxTestSupport.run(() -> {
                ((Stage) field(app, "stage")).close();
                app.stop();
                return null;
            });
        }
    }

    private static void awaitOperation(MainWindow app, Runnable start) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        FxTestSupport.run(() -> {
            BooleanProperty busy = (BooleanProperty) field(app, "busy");
            busy.addListener((obs, wasBusy, isBusy) -> {
                if (wasBusy && !isBusy) Platform.runLater(() -> {
                    FxTestSupport.dismissDialogs(null);
                    done.complete(null);
                });
            });
            start.run();
            assertTrue(busy.get());
            return null;
        });
        done.get(15, TimeUnit.SECONDS);
    }
}
