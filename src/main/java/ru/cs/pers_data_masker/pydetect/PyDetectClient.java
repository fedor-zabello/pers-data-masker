package ru.cs.pers_data_masker.pydetect;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cs.pers_data_masker.pydetect.proto.DetectRequest;
import ru.cs.pers_data_masker.pydetect.proto.DetectResponse;
import ru.cs.pers_data_masker.pydetect.proto.DetectServiceGrpc;
import ru.cs.pers_data_masker.pydetect.proto.Span;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC-клиент к Python-микросервису детекции.
 *
 * <p>Вызывает {@code DetectService.Detect} и возвращает спаны. При недоступности
 * Python-сервиса или превышении таймаута логирует ошибку и возвращает пустой
 * список (graceful degradation) — запрос продолжается с локальными детекторами.
 */
public class PyDetectClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PyDetectClient.class);

    private final ManagedChannel channel;
    private final DetectServiceGrpc.DetectServiceBlockingStub stub;
    private final long timeoutMs;

    public PyDetectClient(PyDetectProperties props) {
        this.channel = ManagedChannelBuilder
                .forAddress(props.getHost(), props.getPort())
                .usePlaintext()
                .build();
        this.stub = DetectServiceGrpc.newBlockingStub(channel);
        this.timeoutMs = props.getTimeoutMs();
    }

    /**
     * Вызывает Python-сервис и возвращает спаны.
     *
     * @param text исходный текст
     * @return список спанов (пустой при недоступности сервиса)
     */
    public List<Span> detect(String text) {
        try {
            DetectRequest request = DetectRequest.newBuilder().setText(text).build();
            DetectResponse response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS).detect(request);
            return response.getSpansList();
        } catch (RuntimeException e) {
            log.warn("Python detect service unavailable, falling back to local detectors: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void close() {
        channel.shutdownNow();
    }
}