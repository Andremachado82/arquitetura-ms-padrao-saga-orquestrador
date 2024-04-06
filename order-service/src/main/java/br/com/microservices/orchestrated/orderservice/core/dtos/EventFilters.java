package br.com.microservices.orchestrated.orderservice.core.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventFilters {

    private String orderId;

    private String transactionId;
}
