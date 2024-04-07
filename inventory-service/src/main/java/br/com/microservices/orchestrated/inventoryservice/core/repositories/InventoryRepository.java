package br.com.microservices.orchestrated.inventoryservice.core.repositories;

import br.com.microservices.orchestrated.inventoryservice.core.models.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    Optional<Inventory> findByProductCode(String productCode);
}
