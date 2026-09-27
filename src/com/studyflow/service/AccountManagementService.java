package com.studyflow.service;

import com.studyflow.model.AuthenticatedUser;
import com.studyflow.model.UserAccount;
import com.studyflow.model.UserRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Persistent authentication and role-aware account administration backend. */
public final class AccountManagementService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final AccountRepository repository = new AccountRepository();
    private final PasswordHasher passwordHasher = new PasswordHasher();
    private final List<UserAccount> accounts = new ArrayList<>();

    public AccountManagementService() {
        accounts.addAll(repository.load());
        if (accounts.isEmpty()) seedInitialAccounts();
    }

    public synchronized Optional<AuthenticatedUser> authenticate(String email, char[] password) {
        String normalizedEmail = normalizeEmail(email);
        return accounts.stream()
                .filter(UserAccount::isActive)
                .filter(account -> account.getEmail().equals(normalizedEmail))
                .filter(account -> passwordHasher.matches(password, account.getPasswordSalt(), account.getPasswordHash()))
                .map(UserAccount::toAuthenticatedUser)
                .findFirst();
    }

    public synchronized UserAccount createStudent(AuthenticatedUser actor, String name, String email,
                                                   String program, char[] temporaryPassword) {
        requireRole(actor, UserRole.MENTOR, UserRole.ADMIN);
        return createAccount(name, email, program, UserRole.STUDENT, temporaryPassword);
    }

    public synchronized UserAccount createMentor(AuthenticatedUser actor, String name, String email,
                                                  char[] temporaryPassword) {
        requireRole(actor, UserRole.ADMIN);
        return createAccount(name, email, "Faculty", UserRole.MENTOR, temporaryPassword);
    }

    public synchronized List<UserAccount> getStudents() {
        return accounts.stream().filter(UserAccount::isActive)
                .filter(account -> account.getRole() == UserRole.STUDENT)
                .sorted(Comparator.comparing(UserAccount::getName))
                .collect(Collectors.toList());
    }

    public synchronized List<UserAccount> getMentors() {
        return accounts.stream().filter(UserAccount::isActive)
                .filter(account -> account.getRole() == UserRole.MENTOR)
                .sorted(Comparator.comparing(UserAccount::getName))
                .collect(Collectors.toList());
    }

    private UserAccount createAccount(String name, String email, String program,
                                      UserRole role, char[] password) {
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = normalizeEmail(email);
        String cleanProgram = program == null ? "" : program.trim();
        if (cleanName.length() < 2) throw new IllegalArgumentException("Enter a valid full name.");
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        if (accounts.stream().anyMatch(account -> account.getEmail().equals(cleanEmail))) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Temporary passwords must contain at least 8 characters.");
        }
        byte[] salt = passwordHasher.newSalt();
        byte[] hash = passwordHasher.hash(password, salt);
        UserAccount account = new UserAccount(cleanName, cleanEmail,
                cleanProgram.isEmpty() ? "BSc Computer Science" : cleanProgram,
                role, salt, hash);
        accounts.add(account);
        repository.save(accounts);
        Arrays.fill(hash, (byte) 0);
        return account;
    }

    private void seedInitialAccounts() {
        seed("Aisha Malik", "student@studyflow.com", "BSc Computer Science", UserRole.STUDENT, "student123");
        seed("Dr. Maya Rao", "mentor@studyflow.com", "Faculty", UserRole.MENTOR, "mentor123");
        seed("StudyFlow Admin", "admin@studyflow.com", "Administration", UserRole.ADMIN, "admin123");
        repository.save(accounts);
    }

    private void seed(String name, String email, String program, UserRole role, String password) {
        char[] characters = password.toCharArray();
        try {
            byte[] salt = passwordHasher.newSalt();
            byte[] hash = passwordHasher.hash(characters, salt);
            accounts.add(new UserAccount(name, email, program, role, salt, hash));
            Arrays.fill(hash, (byte) 0);
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    private static void requireRole(AuthenticatedUser actor, UserRole... allowed) {
        if (actor == null || Arrays.stream(allowed).noneMatch(role -> role == actor.getRole())) {
            throw new SecurityException("Your account does not have permission for this action.");
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ENGLISH);
    }
}
