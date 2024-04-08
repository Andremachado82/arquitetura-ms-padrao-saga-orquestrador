package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import br.com.microservices.orchestrated.orchestratorservice.configs.exceptions.ValidationException;
import br.com.microservices.orchestrated.orchestratorservice.core.dtos.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaHandler.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@AllArgsConstructor
@Component
public class SagaExecutionController {

    public static final String SAGA_LOG_ID = "ORDER ID: %s  | TRANSACTION ID %s | EVENT ID %s";

    public ETopics getNextTopic(Event event) {
        if (isEmpty(event.getStatus()) || isEmpty(event.getSource())) {
            throw new ValidationException("Source and status must be informed");
        }
        var topic = findTopicBySourceAndStatus(event);
        logCurrentSaga(event, topic);
        return topic;
    }

    private ETopics findTopicBySourceAndStatus(Event event) {
        return (ETopics) Arrays.stream(SAGA_HANDLER)
                .filter( row -> isEventSourceANdStatusValid(event, row))
                .map(i -> i[TOPIC_INDEX])
                .findFirst()
                .orElseThrow(() -> new ValidationException("Topic not found"));
    }

    private boolean isEventSourceANdStatusValid(Event event, Object [] row ) {
        var source = row[EVENT_SOURCE_INDEX];
        var status = row[SAGA_STATUS_INDEX];
        return event.getSource().equals(source) && event.getStatus().equals(status);
    }

    private void logCurrentSaga(Event event, ETopics topic) {
        var sagaId = createSagaId(event);
        var source = event.getSource();
        switch (event.getStatus()) {
            case SUCCESS -> log.info("### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case ROLLBACK_PENDING -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case FAIL -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {} ",
                    source, topic, sagaId);
        }
    }

    private String createSagaId(Event event) {
        return String.format(SAGA_LOG_ID, event.getPayload().getId(), event.getTransactionId(), event.getId());
    }
}
