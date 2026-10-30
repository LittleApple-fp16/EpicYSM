package com.argorice.epicysm.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Bounded diagnostic history with durable incident snapshots and exportable logs. */
public final class DiagnosticJournal implements AutoCloseable {
    private static final Gson JSON = new Gson();
    private final Path directory;
    private final int historyLimit;
    private final long byteLimit;
    private final long started = System.nanoTime();
    private final String run = UUID.randomUUID().toString();
    private final Deque<JsonObject> history = new ArrayDeque<>();
    private BufferedWriter writer;
    private long bytes;
    private long sequence;

    public DiagnosticJournal(Path directory, int historyLimit, long byteLimit) throws IOException {
        this.directory = directory;
        this.historyLimit = historyLimit;
        this.byteLimit = byteLimit;
        Files.createDirectories(directory);
        rotate();
    }

    private void rotate() throws IOException {
        if (writer != null) writer.close();
        Files.deleteIfExists(directory.resolve("runtime-3.jsonl"));
        for (int i = 2; i >= 0; i--) {
            Path source = directory.resolve(i == 0 ? "runtime-current.jsonl" : "runtime-" + i + ".jsonl");
            if (Files.exists(source)) Files.move(source, directory.resolve("runtime-" + (i + 1) + ".jsonl"), StandardCopyOption.REPLACE_EXISTING);
        }
        writer = Files.newBufferedWriter(directory.resolve("runtime-current.jsonl"), StandardCharsets.UTF_8);
        bytes = 0;
    }

    public synchronized void record(String kind, JsonObject data) throws IOException {
        JsonObject entry = new JsonObject();
        entry.addProperty("run", run);
        entry.addProperty("sequence", ++sequence);
        entry.addProperty("utc", Instant.now().toString());
        entry.addProperty("elapsed_ms", (System.nanoTime() - started) / 1_000_000);
        entry.addProperty("thread", Thread.currentThread().getName());
        entry.addProperty("kind", kind);
        entry.add("data", data.deepCopy());
        String line = JSON.toJson(entry) + "\n";
        if (bytes > 0 && bytes + line.getBytes(StandardCharsets.UTF_8).length > byteLimit) rotate();
        writer.write(line);
        writer.flush();
        bytes += line.getBytes(StandardCharsets.UTF_8).length;
        history.addLast(entry);
        while (history.size() > historyLimit) history.removeFirst();
    }

    public synchronized Path incident(String reason, JsonObject state) throws IOException {
        JsonObject report = new JsonObject();
        report.addProperty("run", run);
        report.addProperty("reason", reason);
        report.addProperty("utc", Instant.now().toString());
        report.add("snapshot", state.deepCopy());
        JsonArray preceding = new JsonArray();
        history.forEach(entry -> preceding.add(entry.deepCopy()));
        report.add("preceding_events", preceding);
        Path target = directory.resolve("incident-" + System.currentTimeMillis() + "-" + String.format("%012d", sequence) + "-" + UUID.randomUUID() + ".json");
        Files.writeString(target, JSON.toJson(report), StandardCharsets.UTF_8);
        prune("incident-", ".json", 12);
        JsonObject event = new JsonObject();
        event.addProperty("reason", reason);
        event.addProperty("file", target.getFileName().toString());
        record("incident", event);
        return target;
    }

    public synchronized Path export(JsonObject state) throws IOException {
        incident("manual_report", state);
        Path target = directory.resolve("report-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target)); var files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json") || p.getFileName().toString().endsWith(".jsonl")).toList()) {
                zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
        prune("report-", ".zip", 3);
        return target;
    }

    private void prune(String prefix, String suffix, int limit) throws IOException {
        try (var files = Files.list(directory)) {
            var matching = files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(prefix) && p.getFileName().toString().endsWith(suffix))
                    .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).thenComparing(p -> p.getFileName().toString()).reversed()).toList();
            for (int i = limit; i < matching.size(); i++) Files.deleteIfExists(matching.get(i));
        }
    }

    @Override
    public synchronized void close() throws IOException {
        writer.close();
    }
}
