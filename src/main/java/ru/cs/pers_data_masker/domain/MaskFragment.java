package ru.cs.pers_data_masker.domain;

/**
 * Фрагмент соответствия «маска → оригинал».
 *
 * <p>{@code maskStart}/{@code maskEnd} — координаты фрагмента <b>в замаскированной
 * строке</b> (том самом результате, который сервис отдаёт клиенту и который
 * клиент пришлёт обратно на шаге демаскирования).
 *
 * <p>Именно {@code MaskFragment}, а не {@link Span}, сохраняется в
 * {@code CorrelationStore} и используется при демаскировании: координаты
 * {@code Span} в исходном тексте непригодны из-за разной длины оригинала и маски.
 *
 * @param maskStart координата начала в маске (включительно)
 * @param maskEnd   координата конца в маске (исключительно)
 * @param type      имя типа ПД
 * @param original  исходный фрагмент текста
 */
public record MaskFragment(int maskStart, int maskEnd, String type, String original) {

    public int length() {
        return maskEnd - maskStart;
    }
}