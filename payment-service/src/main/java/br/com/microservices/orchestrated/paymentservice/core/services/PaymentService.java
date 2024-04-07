package br.com.microservices.orchestrated.paymentservice.core.services;

import br.com.microservices.orchestrated.paymentservice.configs.exceptions.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dtos.Event;
import br.com.microservices.orchestrated.paymentservice.core.dtos.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.models.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producers.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repositories.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final PaymentRepository paymentRepository;


    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);
        } catch (Exception ex) {
            log.error("Error trying to make payment: ", ex);
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event) {
        Boolean existsTransaction =
                paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId());
        if (existsTransaction) {
            throw new ValidationException("There's another transactionID for this validation");
        }
    }

    private void createPendingPayment(Event event) {
        var totalAmount = totalAmount(event);
        var totalItems = totalItems(event);
        var payment = Payment
                .builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private double totalAmount(Event event) {
        return event.getPayload().getProducts().stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int totalItems(Event event) {
        return event.getPayload().getProducts().stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }
}
