package com.bettercontent.betterwildfruittrees.diagnostics;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Supplementary wiring/privacy invariant, not the actual emitter or native behavior test. */
final class LoggingBoundaryTest {
    @Test void authoritativeFeatureBoundaryAndPrivacyRemainExplicit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/betterwildfruittrees/TreeFruitBudget.java")) +
            Files.readString(Path.of("src/main/java/com/bettercontent/betterwildfruittrees/diagnostics/FeatureObservation.java")) +
            Files.readString(Path.of("src/main/java/com/bettercontent/betterwildfruittrees/diagnostics/FruitBatchObservation.java"));
        assertTrue(source.contains("FruitBatchObservation"), "FruitBatchObservation");
        assertTrue(source.contains("fruit.batch"), "fruit.batch");
        assertTrue(source.contains("native_callback_threw"), "native_callback_threw");
        assertTrue(source.contains("fruit.deferred_batch"), "fruit.deferred_batch");
        assertTrue(source.contains("insufficient_credit"), "insufficient_credit");
        assertFalse(source.contains("fields(\"nbt\""));
        assertFalse(source.contains("fields(\"payload\""));
        assertFalse(source.contains("fields(\"seed\""));
        assertTrue(source.contains("enabled("));
    }
}
