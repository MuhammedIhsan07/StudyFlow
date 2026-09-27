package com.studyflow.service;

import com.studyflow.model.UserAccount;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** File-backed account store for the standalone desktop deployment. */
final class AccountRepository {
    private final Path accountFile = Paths.get(System.getProperty("user.home"),
            ".studyflow", "accounts.bin");

    synchronized List<UserAccount> load() {
        if (!Files.exists(accountFile)) return new ArrayList<>();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(accountFile))) {
            Object value = input.readObject();
            if (value instanceof List<?>) {
                List<UserAccount> accounts = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof UserAccount) accounts.add((UserAccount) item);
                }
                return accounts;
            }
        } catch (IOException | ClassNotFoundException | RuntimeException ignored) {
            // A corrupt store is treated as empty so the recovery seed can be created.
        }
        return new ArrayList<>();
    }

    synchronized void save(List<UserAccount> accounts) {
        try {
            Files.createDirectories(accountFile.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(accountFile))) {
                output.writeObject(new ArrayList<>(accounts));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Account data could not be saved.", exception);
        }
    }
}
