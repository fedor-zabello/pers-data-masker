package ru.cs.pers_data_masker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProcessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void maskThenUnmaskRoundTrip() throws Exception {
        String payloadId = "test-id-1";
        String original = "Иванов Иван Иванович, email ivan@mail.ru";

        String masked = mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + original + "\",\"payload_id\":\"" + payloadId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String result = mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + masked + "\",\"payload_id\":\"" + payloadId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(original))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void validationRejectsBlankPayload() throws Exception {
        mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"\",\"payload_id\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}