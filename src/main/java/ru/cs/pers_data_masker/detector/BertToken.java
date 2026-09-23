package ru.cs.pers_data_masker.detector;

/**
 * Токен WordPiece с id, текстом и координатами в исходном тексте.
 */
public class BertToken {

    /** id токена в vocab. */
    public final long id;
    /** текст токена (может содержать {@code ##} для подслов). */
    public final String text;
    /** координата начала в исходном тексте. */
    public final int start;
    /** координата конца в исходном тексте. */
    public final int end;
    /** исходное слово (до WordPiece-разбиения). */
    public final String original;

    public BertToken(long id, String text, int start, int end, String original) {
        this.id = id;
        this.text = text;
        this.start = start;
        this.end = end;
        this.original = original;
    }
}