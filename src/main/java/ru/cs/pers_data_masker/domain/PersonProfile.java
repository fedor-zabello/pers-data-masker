package ru.cs.pers_data_masker.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Профиль человека: набор спанов ПД, относящихся к одному лицу.
 *
 * <p>Группирует ФИО, дату рождения, место рождения, паспорт, адрес и другие
 * поля, найденные в одном блоке текста. Используется для повышения уверенности
 * связанных полей (наличие нескольких полей одного человека усиливает
 * вероятность того, что это действительно ПД).
 */
public class PersonProfile {

    private final List<Span> spans = new ArrayList<>();

    public void add(Span span) {
        spans.add(span);
    }

    public void replace(int index, Span span) {
        spans.set(index, span);
    }

    public List<Span> spans() {
        return List.copyOf(spans);
    }

    public int size() {
        return spans.size();
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}