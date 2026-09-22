package ru.cs.pers_data_masker.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cs.pers_data_masker.security.SystemIdentificationFilter;
import ru.cs.pers_data_masker.service.ProcessService;

/**
 * Единый контракт {@code POST /process}.
 *
 * <p>Первый запрос с новым {@code payload_id} — маскирование; второй запрос с тем
 * же {@code payload_id} — демаскирование. Ответ всегда {@code {"result": "..."}}.
 */
@RestController
@RequestMapping("/process")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> process(@Valid @RequestBody ProcessRequest request,
                                                   HttpServletRequest httpRequest) {
        String systemId = (String) httpRequest.getAttribute(SystemIdentificationFilter.SYSTEM_ID_ATTRIBUTE);
        String result = processService.process(systemId, request.payload(), request.payloadId());
        return ResponseEntity.ok(new ProcessResponse(result));
    }
}