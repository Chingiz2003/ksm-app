package kz.ksm.fruit.controller;

import jakarta.validation.Valid;
import kz.ksm.fruit.dto.OrderRequest;
import kz.ksm.fruit.model.CustomerOrder;
import kz.ksm.fruit.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<CustomerOrder> findAll() {
        return orderService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SALES','MANAGER')")
    public CustomerOrder create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.create(request, authentication);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES','MANAGER')")
    public CustomerOrder update(@PathVariable Long id, @Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES','MANAGER')")
    public void delete(@PathVariable Long id, Authentication authentication) {
        orderService.delete(id, authentication);
    }
}
