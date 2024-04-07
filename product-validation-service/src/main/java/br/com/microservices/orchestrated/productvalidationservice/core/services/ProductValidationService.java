package br.com.microservices.orchestrated.productvalidationservice.core.services;

import br.com.microservices.orchestrated.productvalidationservice.configs.exceptions.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dtos.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dtos.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dtos.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.models.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producers.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repositories.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repositories.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.SUCCESS;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@AllArgsConstructor
@Service
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistsProducts(Event event) {
        try {
            checkCurrentValidation(event);
            createValidation(event);
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to validate products: ", ex);
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event) {
        validateProductsInformed(event);
        Boolean existsTransaction =
                validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId());
        if (existsTransaction) {
            throw new ValidationException("There's another transactionID for this validation");
        }
        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });

    }

    private void validateProductsInformed(Event event) {
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("Product list is empty!");
        }
        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderID and TransactionID must be informed!");
        }
    }

    private void validateProductInformed(OrderProducts products) {
        if (isEmpty(products.getProduct()) || isEmpty(products.getProduct().getCode())) {
            throw new ValidationException("Product must be informed!");
        }
    }

    private void validateExistingProduct(String code) {
        if (!productRepository.existsByCode(code)) {
            throw new ValidationException("Product does not exists in database!");
        }
    }

    private void createValidation(Event event) {
        var validation = Validation
                .builder()
                .orderId(event.getOrderId())
                .transactionId(event.getTransactionId())
                .success(true)
                .build();
        validationRepository.save(validation);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event);
    }

    private void addHistory(Event event) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message("Products are validated successfully.")
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

}
