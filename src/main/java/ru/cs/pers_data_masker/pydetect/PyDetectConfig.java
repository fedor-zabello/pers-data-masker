package ru.cs.pers_data_masker.pydetect;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация модуля интеграции с Python-микросервисом.
 *
 * <p>Создаёт gRPC-клиент и {@link PyPiiDetector} только если
 * {@code pydetect.enabled=true}. При выключенном модуле работают только
 * встроенные Java-детекторы.
 */
@Configuration
@EnableConfigurationProperties(PyDetectProperties.class)
public class PyDetectConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "pydetect", name = "enabled", havingValue = "true")
    public PyDetectClient pyDetectClient(PyDetectProperties props) {
        return new PyDetectClient(props);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pydetect", name = "enabled", havingValue = "true")
    public PyPiiDetector pyPiiDetector(PyDetectClient pyDetectClient, PyDetectProperties props) {
        return new PyPiiDetector(pyDetectClient, props, 1000);
    }
}