package ru.cs.pers_data_masker.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Упрощённый WordPiece-токенизатор для rubert-tiny.
 *
 * <p>Читает {@code tokenizer.json} (формат Hugging Face tokenizers), извлекает
 * vocab и спецтокены. Разбивает текст на слова, применяет WordPiece-разбиение,
 * добавляет {@code [CLS]} в начало и {@code [SEP]} в конец. Для каждого токена
 * сохраняет координаты в исходном тексте (для восстановления спанов).
 */
public class BertTokenizer {

    private final Map<String, Integer> vocab;
    private final int clsId;
    private final int sepId;
    private final int padId;
    private final String unkToken;
    private final String clsToken;
    private final String sepToken;

    public BertTokenizer(String tokenizerJsonPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (InputStream in = Files.newInputStream(Path.of(tokenizerJsonPath))) {
            root = mapper.readTree(in);
        }

        JsonNode model = root.path("model");
        JsonNode vocabNode = model.path("vocab");
        this.vocab = new HashMap<>();
        vocabNode.fields().forEachRemaining(e -> vocab.put(e.getKey(), e.getValue().asInt()));

        JsonNode addedTokens = root.path("added_tokens");
        String cls = "[CLS]";
        String sep = "[SEP]";
        String unk = "[UNK]";
        for (JsonNode t : addedTokens) {
            String content = t.path("content").asText();
            if ("[CLS]".equals(content)) {
                cls = content;
            } else if ("[SEP]".equals(content)) {
                sep = content;
            } else if ("[UNK]".equals(content)) {
                unk = content;
            }
        }
        this.clsToken = cls;
        this.sepToken = sep;
        this.unkToken = unk;
        this.clsId = vocab.getOrDefault(cls, 101);
        this.sepId = vocab.getOrDefault(sep, 102);
        this.padId = vocab.getOrDefault("[PAD]", 0);
    }

    /**
     * Токенизирует текст, возвращая токены с id и координатами в исходном тексте.
     * Первый токен — {@code [CLS]}, последний — {@code [SEP]}.
     */
    public List<BertToken> tokenize(String text) {
        List<BertToken> result = new ArrayList<>();
        result.add(new BertToken(clsId, clsToken, 0, 0, ""));

        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int wordStart = i;
            while (i < n && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            String word = text.substring(wordStart, i);
            wordPiece(word, wordStart, result);
        }

        result.add(new BertToken(sepId, sepToken, n, n, ""));
        return result;
    }

    private void wordPiece(String word, int offset, List<BertToken> result) {
        String lower = word.toLowerCase();
        if (vocab.containsKey(lower)) {
            result.add(new BertToken(vocab.get(lower), lower, offset, offset + word.length(), word));
            return;
        }
        List<String> pieces = new ArrayList<>();
        String current = lower;
        while (!current.isEmpty()) {
            String candidate = current;
            while (!candidate.isEmpty() && !vocab.containsKey(candidate)) {
                candidate = candidate.substring(0, candidate.length() - 1);
            }
            if (candidate.isEmpty()) {
                pieces.add(unkToken);
                break;
            }
            pieces.add(candidate);
            current = current.substring(candidate.length());
        }
        int cursor = offset;
        for (String piece : pieces) {
            int len = piece.length();
            result.add(new BertToken(vocab.getOrDefault(piece, vocab.getOrDefault(unkToken, 100)),
                    piece, cursor, cursor + len, word));
            cursor += len;
        }
    }
}