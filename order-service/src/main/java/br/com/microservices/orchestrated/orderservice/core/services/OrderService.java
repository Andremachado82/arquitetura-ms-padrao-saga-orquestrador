package br.com.microservices.orchestrated.orderservice.core.services;

import br.com.microservices.orchestrated.orderservice.core.documents.Event;
import br.com.microservices.orchestrated.orderservice.core.documents.Order;
import br.com.microservices.orchestrated.orderservice.core.dtos.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.producers.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repositories.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private final EventService eventService;
    private final SagaProducer sagaProducer;
    private static final String TRANSACTION_ID_PATTERN = "%s_%s";
    private final JsonUtil jsonUtil;
    private final OrderRepository orderRepository;

    public Order createOrder(OrderRequest orderRequest) {
        var order = Order
                .builder()
                .products(orderRequest.getProducts())
                .createdAt(LocalDateTime.now())
                .transactionId(
                        String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID())
                )
                .build();

        orderRepository.save(order);
        sagaProducer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

    private Event createPayload(Order order) {
        var event = Event.builder()
                .orderId(order.getId())
                .transactionId(order.getTransactionId())
                .payload(order)
                .createdAt(LocalDateTime.now())
                .build();
        eventService.save(event);
        return event;
    }
}
