package br.com.microservices.orchestrated.inventoryservice.core.services;

import br.com.microservices.orchestrated.inventoryservice.configs.exceptions.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dtos.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dtos.History;
import br.com.microservices.orchestrated.inventoryservice.core.dtos.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dtos.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.models.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.models.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producers.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repositories.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repositories.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@Service
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(Event event) {
        try {
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to update inventory: ", ex);
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event) {
        Boolean existsTransaction =
                orderInventoryRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId());
        if (existsTransaction) {
            throw new ValidationException("There's another transactionID for this validation.");
        }
    }


    private void createOrderInventory(Event event) {
        event.getPayload().getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProducts().getCode());
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
                });
    }

    private Inventory findInventoryByProductCode(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found by informed product."));
    }

    private OrderInventory createOrderInventory(Event event, OrderProducts product, Inventory inventory) {
        int newQuantity = inventory.getAvailable() - product.getQuantity();
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(newQuantity)
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }


    private void updateInventory(Order payload) {
        payload.getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProducts().getCode());
                    checkinventory(inventory.getAvailable(), product.getQuantity());
                    inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                    inventoryRepository.save(inventory);
                });
    }

    private void checkinventory(int available, int orderQuantity) {
        if(orderQuantity > available) {
            throw new ValidationException("Product is out of stock");
        }
    }

    private void handleSuccess(Event event) {
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory updated successfully!");
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

}
