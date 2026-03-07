package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.dto.OrderRequest;
import com.chatbox.chatbox.model.BagTemplate;
import com.chatbox.chatbox.model.Order;
import com.chatbox.chatbox.repository.BagTemplateRepository;
import com.chatbox.chatbox.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final BagTemplateRepository bagTemplateRepository;

    public OrderController(OrderRepository orderRepository, BagTemplateRepository bagTemplateRepository) {
        this.orderRepository = orderRepository;
        this.bagTemplateRepository = bagTemplateRepository;
    }

    /** Public: create order */
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderRequest req) {
        if (req.getBagTemplateId() == null || req.getDesignSnapshot() == null || req.getDesignSnapshot().isBlank()
                || req.getCustomerName() == null || req.getCustomerName().isBlank()
                || req.getCustomerPhone() == null || req.getCustomerPhone().isBlank()
                || req.getCustomerAddress() == null || req.getCustomerAddress().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        BagTemplate template = bagTemplateRepository.findById(req.getBagTemplateId())
                .orElse(null);
        if (template == null || !template.getActive()) {
            return ResponseEntity.badRequest().build();
        }
        int qty = req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1;
        double total = template.getBasePrice() * qty;
        Order order = Order.builder()
                .bagTemplateId(req.getBagTemplateId())
                .designSnapshot(req.getDesignSnapshot())
                .customerName(req.getCustomerName().trim())
                .customerPhone(req.getCustomerPhone().trim())
                .customerAddress(req.getCustomerAddress().trim())
                .customerEmail(req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank() ? req.getCustomerEmail().trim() : null)
                .quantity(qty)
                .totalPrice(total)
                .status(Order.OrderStatus.PENDING)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    /** Public: get order by id (for customer lookup) */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
