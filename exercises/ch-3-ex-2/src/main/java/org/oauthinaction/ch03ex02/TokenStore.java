package org.oauthinaction.ch03ex02;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** A replacement for the book's shared NoSQL file: one writer, multiple readers. */
public final class TokenStore {
    private final Path file;
    private final ObjectMapper json = new ObjectMapper();

    public TokenStore(Path file) { this.file = file.toAbsolutePath(); }

    public static TokenStore local() {
        return new TokenStore(Path.of(System.getProperty("java.io.tmpdir"),
                "oauth-in-action", "ch-3-ex-2", "tokens.json"));
    }

    public synchronized void clear() { write(Map.of()); }

    public synchronized void put(String token, String clientId, String scope) {
        var tokens = read();
        tokens.put(token, new Token(clientId, scope, false));
        write(tokens);
    }

    public synchronized void putRefresh(String token, String clientId, String scope) {
        var tokens = read();
        tokens.put(token, new Token(clientId, scope, true));
        write(tokens);
    }

    public synchronized Token findRefresh(String token) {
        Token stored = token == null ? null : read().get(token);
        return stored != null && stored.refresh() ? stored : null;
    }

    public synchronized boolean contains(String token) {
        Token stored = token == null || token.isBlank() ? null : read().get(token);
        return stored != null && !stored.refresh();
    }

    private Map<String, Token> read() {
        try {
            if (!Files.exists(file)) return new LinkedHashMap<>();
            return json.readValue(file.toFile(), new TypeReference<LinkedHashMap<String, Token>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read exercise token file: " + file, e);
        }
    }

    private void write(Map<String, Token> tokens) {
        try {
            Files.createDirectories(file.getParent());
            Path pending = file.resolveSibling(file.getFileName() + ".pending");
            json.writerWithDefaultPrettyPrinter().writeValue(pending.toFile(), tokens);
            // Readers see either the old complete file or the new complete file.
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write exercise token file: " + file, e);
        }
    }

    public record Token(String clientId, String scope, boolean refresh) {}
}
