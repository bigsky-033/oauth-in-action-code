package org.oauthinaction.ch06ex01;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;

/** One authorization-server writer; resource servers read the same atomic file. */
public final class TokenStore {
    private final Path file;
    private final ObjectMapper json = new ObjectMapper();
    public TokenStore(Path file) { this.file = file.toAbsolutePath(); }
    public static TokenStore local() {
        return new TokenStore(Path.of(System.getProperty("java.io.tmpdir"), "oauth-in-action", "ch-6-ex-1", "tokens.json"));
    }
    public synchronized void clear() { write(Map.of()); }
    public synchronized void save(String token, Token value) {
        var values = read();
        values.put(token, value);
        write(values);
    }
    public void put(String token, String clientId, String scope) {
        save(token, new Token(clientId, scopes(scope), null, false));
    }
    public synchronized Token access(String token) {
        Token value = token == null ? null : read().get(token);
        return value == null || value.refresh() ? null : value;
    }
    public synchronized Token refresh(String token) {
        Token value = token == null ? null : read().get(token);
        return value != null && value.refresh() ? value : null;
    }
    public boolean contains(String token) { return access(token) != null; }
    public static List<String> scopes(String scope) {
        return scope == null || scope.isBlank() ? List.of() : List.of(scope.trim().split("\\s+"));
    }
    private Map<String, Token> read() {
        try {
            return Files.exists(file) ? json.readValue(file.toFile(), new TypeReference<LinkedHashMap<String, Token>>() {})
                    : new LinkedHashMap<>();
        } catch (IOException e) { throw new UncheckedIOException("Cannot read " + file, e); }
    }
    private void write(Map<String, Token> values) {
        try {
            Files.createDirectories(file.getParent());
            Path pending = file.resolveSibling(file.getFileName() + ".pending");
            json.writerWithDefaultPrettyPrinter().writeValue(pending.toFile(), values);
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { throw new UncheckedIOException("Cannot write " + file, e); }
    }
    public record Token(String clientId, List<String> scope, String user, boolean refresh) {}
}
