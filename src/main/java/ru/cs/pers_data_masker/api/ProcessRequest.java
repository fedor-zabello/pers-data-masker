package ru.cs.pers_data_masker.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос к {@code POST /process}.
 *
 * @param payload   строка для обработки (исходная или маска)
 * @param payloadId идентификатор пары маскирование→демаскирование
 */
public record ProcessRequest(
        @NotBlank(message = "payload must not be blank")
        @Size(max = 1_000_000, message = "payload too large")
        String payload,

        @NotBlank(message = "payload_id must not be blank")
        @Size(max = 256, message = "payload_id too long")
        @JsonProperty("payload_id")
        String payloadId
) {
}