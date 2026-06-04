package kz.ksm.fruit.service;

import kz.ksm.fruit.dto.FinanceSummaryDto;
import kz.ksm.fruit.dto.ProfitRowDto;
import kz.ksm.fruit.model.CustomerOrder;
import kz.ksm.fruit.model.OrderStatus;
import kz.ksm.fruit.repository.CustomerOrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceService {
    private final CustomerOrderRepository orderRepository;

    public FinanceService(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public FinanceSummaryDto summarize(Authentication authentication) {
        List<CustomerOrder> orders = orderRepository.findByStatus(OrderStatus.SUPPLIER_SELECTED);
        if (hasRole(authentication, "ROLE_SALES") && !hasRole(authentication, "ROLE_MANAGER")) {
            orders = orders.stream()
                    .filter(order -> authentication.getName().equals(order.getManagerUsername()))
                    .toList();
        }

        BigDecimal totalRevenue = sum(orders, true);
        BigDecimal totalProfit = sum(orders, false);
        return new FinanceSummaryDto(
                totalRevenue,
                totalProfit,
                grouped(orders, CustomerOrder::getFruitName),
                grouped(orders, CustomerOrder::getRestaurant),
                grouped(orders, order -> order.getOrderDate().toString()).stream()
                        .sorted(Comparator.comparing(ProfitRowDto::name))
                        .toList()
        );
    }

    private List<ProfitRowDto> grouped(List<CustomerOrder> orders, java.util.function.Function<CustomerOrder, String> classifier) {
        Map<String, List<CustomerOrder>> grouped = orders.stream().collect(Collectors.groupingBy(classifier));
        return grouped.entrySet().stream()
                .map(entry -> new ProfitRowDto(entry.getKey(), sum(entry.getValue(), true), sum(entry.getValue(), false)))
                .sorted(Comparator.comparing(ProfitRowDto::profit).reversed())
                .toList();
    }

    private BigDecimal sum(List<CustomerOrder> orders, boolean revenue) {
        return orders.stream()
                .map(order -> revenue ? order.getOrderAmount() : order.getProfitAmount())
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
