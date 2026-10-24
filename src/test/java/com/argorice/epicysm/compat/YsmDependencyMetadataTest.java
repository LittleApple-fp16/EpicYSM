package com.argorice.epicysm.compat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YsmDependencyMetadataTest {
    @Test
    void providerDependenciesPermitEitherModOnEachLoader() throws IOException {
        Path directory = Path.of("src/main/resources/META-INF");
        for (String file : new String[] { "mods.toml", "neoforge.mods.toml" }) {
            Path metadata = directory.resolve(file);
            if (!Files.exists(metadata)) {
                continue;
            }
            String source = Files.readString(metadata);
            for (String modId : YsmProvider.MOD_IDS) {
                var block = Pattern.compile("(?s)modId\\s*=\\s*\"" + modId + "\"(.*?)(?=\\[\\[|$)").matcher(source);
                assertTrue(block.find(), "Missing provider dependency: " + modId);
                assertTrue(block.group(1).contains(file.equals("mods.toml") ? "mandatory = false" : "type = \"optional\""),
                        "Provider must be optional in loader metadata: " + modId);
                assertTrue(block.group(1).contains("side = \"CLIENT\""));
                assertTrue(block.group(1).contains("ordering = \"AFTER\""));
            }
        }
    }
}
