package ru.cs.pers_data_masker.detector;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.Span;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Клиент NER-модели (ONNX Runtime + rubert-tiny).
 *
 * <p>Загружает модель и токенизатор один раз при старте, выполняет инференс
 * на виртуальных потоках. Возвращает спаны типа {@code PER} (ФИО) и
 * {@code ORG} (организации) с координатами в исходном тексте.
 *
 * <p>Модель: {@code onnx-community/ner-rubert-tiny-news-ONNX} (int8, ~29 МБ).
 * Лейблы: {@code B-PER}/{@code I-PER} (id 9/10), {@code B-ORG}/{@code I-ORG}
 * (id 7/8), {@code B-LOC}/{@code I-LOC} (id 5/6).
 */
@Component
public class NerModelClient {

    private static final Logger log = LoggerFactory.getLogger(NerModelClient.class);

    private static final int PER_B = 9;
    private static final int PER_I = 10;
    private static final int ORG_B = 7;
    private static final int ORG_I = 8;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final BertTokenizer tokenizer;
    private final boolean enabled;

    public NerModelClient(@Value("${pii.ner.enabled:false}") boolean enabled,
                          @Value("${pii.ner.model-path:classpath:ner/model.onnx}") String modelPath,
                          @Value("${pii.ner.tokenizer-path:classpath:ner/tokenizer.json}") String tokenizerPath) {
        this.enabled = enabled;
        if (!enabled) {
            this.env = null;
            this.session = null;
            this.tokenizer = null;
            log.info("NER disabled");
            return;
        }
        try {
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            this.session = env.createSession(resolvePath(modelPath), options);
            this.tokenizer = new BertTokenizer(resolvePath(tokenizerPath));
            log.info("NER model loaded: {} inputs={}", modelPath, session.getInputInfo().keySet());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load NER model", e);
        }
    }

    private static String resolvePath(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resource = path.substring("classpath:".length());
            try (InputStream in = NerModelClient.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + resource);
                }
                Path tmp = Files.createTempFile("ner-model-", ".onnx");
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toString();
            }
        }
        return path;
    }

    /**
     * Находит ФИО (PER) в тексте.
     *
     * @param text исходный текст
     * @return спаны типа {@code PER} (тип = "FULL_NAME")
     */
    public List<Span> detectPersons(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return List.of();
        }
        return detect(text, PER_B, PER_I, "FULL_NAME");
    }

    /**
     * Находит организации (ORG) в тексте.
     *
     * @param text исходный текст
     * @return спаны типа {@code ORG} (тип = "BANK_NAME")
     */
    public List<Span> detectOrganizations(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return List.of();
        }
        return detect(text, ORG_B, ORG_I, "BANK_NAME");
    }

    private List<Span> detect(String text, int beginLabel, int insideLabel, String type) {
        try {
            List<BertToken> tokens = tokenizer.tokenize(text);
            if (tokens.isEmpty()) {
                return List.of();
            }
            long[] inputIds = tokens.stream().mapToLong(t -> t.id).toArray();
            long[] attentionMask = new long[inputIds.length];
            java.util.Arrays.fill(attentionMask, 1L);
            long[] tokenTypeIds = new long[inputIds.length];

            try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, new long[][]{inputIds});
                 OnnxTensor attentionTensor = OnnxTensor.createTensor(env, new long[][]{attentionMask});
                 OnnxTensor tokenTypeTensor = OnnxTensor.createTensor(env, new long[][]{tokenTypeIds})) {

                Map<String, OnnxTensor> inputs = Map.of(
                        "input_ids", inputIdsTensor,
                        "attention_mask", attentionTensor,
                        "token_type_ids", tokenTypeTensor);

                try (OrtSession.Result result = session.run(inputs)) {
                    float[][][] logits = (float[][][]) result.get(0).getValue();
                    return decode(logits[0], tokens, beginLabel, insideLabel, type);
                }
            }
        } catch (Exception e) {
            log.warn("NER inference failed, skipping", e);
            return List.of();
        }
    }

    private List<Span> decode(float[][] logits, List<BertToken> tokens,
                              int beginLabel, int insideLabel, String type) {
        List<Span> spans = new ArrayList<>();
        int start = -1;
        int end = -1;
        for (int i = 0; i < logits.length; i++) {
            int label = argmax(logits[i]);
            BertToken token = tokens.get(i);
            if (label == beginLabel || label == insideLabel) {
                if (start == -1) {
                    start = token.start;
                }
                end = token.end;
            } else {
                if (start != -1) {
                    spans.add(new Span(start, end, type, textOf(tokens, start, end), 0.9));
                    start = -1;
                }
            }
        }
        if (start != -1) {
            spans.add(new Span(start, end, type, textOf(tokens, start, end), 0.9));
        }
        return spans;
    }

    private static String textOf(List<BertToken> tokens, int start, int end) {
        for (BertToken t : tokens) {
            if (t.start == start) {
                return t.original;
            }
        }
        return "";
    }

    private static int argmax(float[] logits) {
        int best = 0;
        float bestVal = logits[0];
        for (int i = 1; i < logits.length; i++) {
            if (logits[i] > bestVal) {
                bestVal = logits[i];
                best = i;
            }
        }
        return best;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
