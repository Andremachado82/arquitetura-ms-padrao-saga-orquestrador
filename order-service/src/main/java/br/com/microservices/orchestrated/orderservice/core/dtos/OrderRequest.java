package br.com.microservices.orchestrated.orderservice.core.dtos;

import br.com.microservices.orchestrated.orderservice.core.documents.OrderProducts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private List<OrderProducts> products;
}
