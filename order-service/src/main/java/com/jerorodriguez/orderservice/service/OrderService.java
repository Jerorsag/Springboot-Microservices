package com.jerorodriguez.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerorodriguez.orderservice.dto.InventoryResponse;
import com.jerorodriguez.orderservice.dto.OrderLineItemsDto;
import com.jerorodriguez.orderservice.dto.OrderRequest;
import com.jerorodriguez.orderservice.event.OrderPlacedEvent;
import com.jerorodriguez.orderservice.model.Order;
import com.jerorodriguez.orderservice.model.OrderLineItems;
import com.jerorodriguez.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    @Autowired
    private final KafkaTemplate<String, String> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try(Tracer.SpanInScope isSpanInScope = tracer.withSpan(inventoryServiceLookup.start())) {
            // Call Inventory Service, and place order if product is in stock
            try {
                InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

                if (inventoryResponsArray == null || inventoryResponsArray.length == 0) {
                    throw new IllegalStateException("Error checking inventory status");
                }

                boolean allProductsInStock = Arrays.stream(inventoryResponsArray)
                        .allMatch(InventoryResponse::isInStock);

                if(allProductsInStock) {
                    orderRepository.save(order);
                    // Cuando envías el mensaje
                    ObjectMapper objectMapper = new ObjectMapper();
                    String orderPayload = objectMapper.writeValueAsString(
                            Map.of("orderNumber", order.getOrderNumber())
                    );
                    kafkaTemplate.send("notificationTopic", orderPayload);
                    log.info("Sent message to Kafka: {}", orderPayload);
                    return "Order Placed Successfully";
                } else {
                    throw new IllegalStateException("Product is not in stock, please try again later.");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error processing your order: " + e.getMessage());
            }
        } finally {
            inventoryServiceLookup.end();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
