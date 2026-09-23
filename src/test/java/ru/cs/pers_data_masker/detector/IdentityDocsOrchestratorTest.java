package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdentityDocsOrchestratorTest {

    @Autowired
    private DetectorOrchestrator orchestrator;

    @Test
    void detectsSeamanPassport() {
        List<Span> spans = orchestrator.detect(
                "Паспорт моряка АБ 3456789", Set.of("SEAMAN_PASSPORT"));
        assertThat(spans).anyMatch(s -> s.type().equals("SEAMAN_PASSPORT"));
    }

    @Test
    void detectsMilitaryId() {
        List<Span> spans = orchestrator.detect(
                "Военный билет АБ 3456789", Set.of("MILITARY_ID"));
        assertThat(spans).anyMatch(s -> s.type().equals("MILITARY_ID"));
    }
}