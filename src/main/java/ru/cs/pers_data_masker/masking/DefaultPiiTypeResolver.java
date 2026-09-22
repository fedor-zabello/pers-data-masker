package ru.cs.pers_data_masker.masking;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.config.CustomPiiTypeRegistry;

/**
 * Реализация {@link PiiTypeResolver}: объединяет встроенные типы (дефолтная
 * стратегия) и кастомные типы из YAML (из {@link CustomPiiTypeRegistry}).
 */
@Component
public class DefaultPiiTypeResolver implements PiiTypeResolver {

    private final DefaultMaskingStrategy defaultStrategy;
    private final CustomPiiTypeRegistry customRegistry;

    public DefaultPiiTypeResolver(DefaultMaskingStrategy defaultStrategy,
                                  CustomPiiTypeRegistry customRegistry) {
        this.defaultStrategy = defaultStrategy;
        this.customRegistry = customRegistry;
    }

    @Override
    public MaskingStrategy strategyFor(String type) {
        MaskingStrategy custom = customRegistry.strategyFor(type);
        return custom != null ? custom : defaultStrategy;
    }
}