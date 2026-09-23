package ru.cs.pers_data_masker.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaskUnmaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void maskThenUnmaskRestoresOriginal() throws Exception {
        String payloadId = "it-" + System.nanoTime();
        String original = "Иванов Иван Иванович, телефон +7 (900) 123-45-67, email ivan@mail.ru";

        MvcResult maskResult = mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + original + "\",\"payload_id\":\"" + payloadId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isNotEmpty())
                .andReturn();

        String masked = JsonPath.read(maskResult.getResponse().getContentAsString(), "$.result");
        assertThat(masked).isNotEqualTo(original);

        MvcResult unmaskResult = mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + masked + "\",\"payload_id\":\"" + payloadId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String restored = JsonPath.read(unmaskResult.getResponse().getContentAsString(), "$.result");
        assertThat(restored).isEqualTo(original);
    }
}