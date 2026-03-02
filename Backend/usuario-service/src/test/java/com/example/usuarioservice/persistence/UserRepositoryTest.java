package com.example.usuarioservice.persistence;

import com.example.usuarioservice.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRepository - JSON persistence behavior")
class UserRepositoryTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private Path tempDir;

    @AfterEach
    void cleanup() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("init crea archivo cuando no existe")
    void init_createsFile_whenMissing() throws Exception {
        tempDir = Files.createTempDirectory("users-test-");
        Path jsonPath = tempDir.resolve("data/users.json");

        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(jsonPath.toString());

        // init should create parent dirs and file
        repo.init();

        assertTrue(Files.exists(jsonPath));
        assertNotNull(repo.findAll());
    }

    @Test
    @DisplayName("init carga archivo existente y asigna ids cuando son nulos")
    void init_loadsExistingFile_andAssignsIds() throws Exception {
        tempDir = Files.createTempDirectory("users-test-");
        Path jsonPath = tempDir.resolve("users.json");

        // create a user with null id
        User u = new User(null, "Test", "pw", "t@example.com", true);
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), Collections.singletonList(u));

        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(jsonPath.toString());
        repo.init();

        Collection<User> all = repo.findAll();
        assertEquals(1, all.size());
        User loaded = all.iterator().next();
        assertNotNull(loaded.getId());
        assertEquals("Test", loaded.getName());
    }

    @Test
    @DisplayName("partialUpdate actualiza solo keys provistas")
    void partialUpdate_updatesOnlyProvidedKeys() throws Exception {
        tempDir = Files.createTempDirectory("users-test-");
        Path jsonPath = tempDir.resolve("users.json");

        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(jsonPath.toString());
        repo.init();

        User u = new User(null, "Before", "pw", "b@example.com", true);
        User saved = repo.save(u);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "After");
        updates.put("active", "false");

        User updated = repo.partialUpdate(saved.getId(), updates);

        assertNotNull(updated);
        assertEquals("After", updated.getName());
        assertFalse(updated.isActive());
        // password should remain unchanged
        assertEquals("pw", updated.getPassword());
    }
}
