package com.argorice.epicysm.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticJournalTest {
    @TempDir Path directory;

    private static JsonObject data(int value) {
        JsonObject out = new JsonObject();
        out.addProperty("value", value);
        return out;
    }

    @Test
    void incidentsRetainBoundedPrecedingHistory() throws Exception {
        try (var journal = new DiagnosticJournal(directory, 3, 100000)) {
            for (int i = 0; i < 8; i++) journal.record("frame", data(i));
            Path file = journal.incident("bridge_disabled", data(99));
            var report = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            assertEquals(3, report.getAsJsonArray("preceding_events").size());
            assertEquals(5, report.getAsJsonArray("preceding_events").get(0).getAsJsonObject().getAsJsonObject("data").get("value").getAsInt());
            assertEquals(99, report.getAsJsonObject("snapshot").get("value").getAsInt());
        }
    }

    @Test
    void recordedDataIsIndependentOfLaterMutations() throws Exception {
        try (var journal = new DiagnosticJournal(directory, 5, 100000)) {
            JsonObject mutable = data(1);
            journal.record("before", mutable);
            mutable.addProperty("value", 2);
            var report = JsonParser.parseString(Files.readString(journal.incident("after", data(3)))).getAsJsonObject();
            assertEquals(1, report.getAsJsonArray("preceding_events").get(0).getAsJsonObject().getAsJsonObject("data").get("value").getAsInt());
        }
    }

    @Test
    void timelineSurvivesGameRestartAndRotation() throws Exception {
        try (var journal = new DiagnosticJournal(directory, 5, 100000)) { journal.record("first_run", data(1)); }
        try (var journal = new DiagnosticJournal(directory, 5, 100000)) { journal.record("second_run", data(2)); }
        assertTrue(Files.readString(directory.resolve("runtime-1.jsonl")).contains("first_run"));
        assertTrue(Files.readString(directory.resolve("runtime-current.jsonl")).contains("second_run"));
    }

    @Test
    void logFilesAndIncidentFilesStayBounded() throws Exception {
        try (var journal = new DiagnosticJournal(directory, 3, 400)) {
            for (int i = 0; i < 20; i++) {
                journal.record("rotation", data(i));
                journal.incident("bounded", data(i));
            }
        }
        try (var files = Files.list(directory)) { assertTrue(files.filter(p -> p.toString().endsWith(".jsonl")).count() <= 4); }
        try (var files = Files.list(directory)) { assertEquals(12, files.filter(p -> p.toString().endsWith(".json")).count()); }
    }

    @Test
    void zipContainsFlushedTimelineAndDetailedReport() throws Exception {
        try (var journal = new DiagnosticJournal(directory, 5, 100000)) {
            journal.record("world_changed", data(1));
            Path report = journal.export(data(2));
            try (var zip = new ZipFile(report.toFile())) {
                assertNotNull(zip.getEntry("runtime-current.jsonl"));
                assertEquals(1, zip.stream().filter(e -> e.getName().startsWith("incident-")).count());
                String log = new String(zip.getInputStream(zip.getEntry("runtime-current.jsonl")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(log.contains("world_changed"));
                assertTrue(log.contains("manual_report"));
            }
        }
    }
}
