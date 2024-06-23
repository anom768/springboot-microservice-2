package com.bangkitanom.order.service;

import com.bangkitanom.order.dto.InventoryResponse;
import com.bangkitanom.order.dto.OrderLineItemsDto;
import com.bangkitanom.order.dto.OrderRequest;
import com.bangkitanom.order.event.OrderPlacedEvent;
import com.bangkitanom.order.model.Order;
import com.bangkitanom.order.model.OrderLineItems;
import com.bangkitanom.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import brave.Span;
import brave.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList()
                .stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        log.info("Calling inventory service...");
        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(inventoryServiceLookup.start())) {
            // Todo: check stock to inventory service and save order if in stock
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductInStock = Arrays.stream(inventoryResponses)
                    .allMatch(InventoryResponse::isInStock);
            System.out.println(allProductInStock);

            if (allProductInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("product is not in stock, please try again later");
            }
        } finally {
            inventoryServiceLookup.flush();
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        return orderLineItems;
    }

}
