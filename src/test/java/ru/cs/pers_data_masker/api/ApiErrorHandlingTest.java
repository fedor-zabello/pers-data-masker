package ru.cs.pers_data_masker.api;

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
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/process")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"payload\":\"text\",\"payload_id\":\"id\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void blankPayloadReturns400() throws Exception {
        mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"\",\"payload_id\":\"id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingPayloadIdReturns400() throws Exception {
        mockMvc.perform(post("/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}