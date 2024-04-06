package br.com.microservices.orchestrated.orderservice.core.repositories;

import br.com.microservices.orchestrated.orderservice.core.documents.Event;
import br.com.microservices.orchestrated.orderservice.core.documents.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
}
