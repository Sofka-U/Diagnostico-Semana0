package com.example.usuarioservice.persistence;

import com.example.usuarioservice.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest {

    @Test
    void init_creates_file_when_missing(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("users.json");
        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(file.toString());

        // ensure file does not exist
        assertThat(file.toFile().exists()).isFalse();

        repo.init();

        assertThat(file.toFile().exists()).isTrue();
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void init_loads_existing_file(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("users.json");
        ObjectMapper om = new ObjectMapper();

        User u = new User(10, "Loaded", "pw", "load@example.com", true);
        om.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), List.of(u));

        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(file.toString());

        repo.init();

        User found = repo.findById(10);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Loaded");
    }

    @Test
    void save_and_partialUpdate_apply_updates(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("users.json");
        UserRepository repo = new UserRepository();
        repo.setFilePathForTests(file.toString());
        repo.init();

        User newUser = new User(null, "NewUser", "s", "new@example.com", true);
        User saved = repo.save(newUser);

        assertThat(saved.getId()).isNotNull();
        int id = saved.getId();

        User byEmail = repo.findByEmail("new@example.com");
        assertThat(byEmail).isNotNull();
        assertThat(byEmail.getId()).isEqualTo(id);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Changed");
        updates.put("active", "false");

        User patched = repo.partialUpdate(id, updates);
        assertThat(patched.getName()).isEqualTo("Changed");
        assertThat(patched.isActive()).isFalse();
    }
}
