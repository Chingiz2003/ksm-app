package kz.ksm.fruit.service;

import kz.ksm.fruit.dto.OrderRequest;
import kz.ksm.fruit.model.AuditAction;
import kz.ksm.fruit.model.CustomerOrder;
import kz.ksm.fruit.model.OrderStatus;
import kz.ksm.fruit.model.Supplier;
import kz.ksm.fruit.repository.CustomerOrderRepository;
import kz.ksm.fruit.repository.SupplierRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {
    private static final BigDecimal MARKUP_MULTIPLIER = new BigDecimal("1.25");

    private final CustomerOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public OrderService(CustomerOrderRepository orderRepository, SupplierRepository supplierRepository, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findAll() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(CustomerOrder::getId))
                .toList();
    }

    @Transactional
    public CustomerOrder create(OrderRequest request, String managerUsername) {
        CustomerOrder order = new CustomerOrder();
        applyEditableFields(order, request);
        order.setManagerUsername(managerUsername);
        selectSupplierAndCalculate(order);
        return orderRepository.save(order);
    }

    @Transactional
    public CustomerOrder create(OrderRequest request, Authentication authentication) {
        CustomerOrder saved = create(request, authentication.getName());
        auditService.record(authentication, AuditAction.CREATE, "Заказ", saved.getId(), saved.getRestaurant(),
                "Создан заказ на " + saved.getFruitName() + ": " + saved.getStatus().getLabel());
        return saved;
    }

    @Transactional
    public CustomerOrder update(Long id, OrderRequest request, Authentication authentication) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        restoreStock(order);
        applyEditableFields(order, request);
        selectSupplierAndCalculate(order);
        CustomerOrder saved = orderRepository.save(order);
        auditService.record(authentication, AuditAction.UPDATE, "Заказ", saved.getId(), saved.getRestaurant(),
                "Обновлен заказ на " + saved.getFruitName() + ": " + saved.getStatus().getLabel());
        return saved;
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        restoreStock(order);
        orderRepository.delete(order);
        auditService.record(authentication, AuditAction.DELETE, "Заказ", id, order.getRestaurant(),
                "Удален заказ на " + order.getFruitName() + " для " + order.getRestaurant());
    }

    @Transactional
    public CustomerOrder recalculate(CustomerOrder order) {
        restoreStock(order);
        selectSupplierAndCalculate(order);
        return orderRepository.save(order);
    }

    private void applyEditableFields(CustomerOrder order, OrderRequest request) {
        order.setRestaurant(request.restaurant().trim());
        order.setFruitName(request.fruitName().trim());
        order.setQuantityKg(request.quantityKg().setScale(2, RoundingMode.HALF_UP));
        order.setOrderDate(request.orderDate());
        order.setDeliveryDate(request.deliveryDate());
    }

    private void selectSupplierAndCalculate(CustomerOrder order) {
        clearCalculatedFields(order);
        long availableDays = ChronoUnit.DAYS.between(order.getOrderDate(), order.getDeliveryDate());
        if (availableDays < 0) {
            order.setStatus(OrderStatus.MANUAL_REVIEW);
            return;
        }

        supplierRepository.findByFruitNameIgnoreCase(order.getFruitName()).stream()
                .filter(supplier -> supplier.getStockKg().compareTo(order.getQuantityKg()) >= 0)
                .filter(supplier -> supplier.getDeliveryDays() <= availableDays)
                .min(Comparator
                        .comparing(Supplier::getPurchasePrice)
                        .thenComparing(Supplier::getId))
                .ifPresentOrElse(
                        supplier -> applySupplier(order, supplier),
                        () -> order.setStatus(OrderStatus.MANUAL_REVIEW)
                );
    }

    private void applySupplier(CustomerOrder order, Supplier supplier) {
        BigDecimal purchasePrice = supplier.getPurchasePrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal salePrice = purchasePrice.multiply(MARKUP_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = order.getQuantityKg().multiply(purchasePrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = order.getQuantityKg().multiply(salePrice).setScale(2, RoundingMode.HALF_UP);

        supplier.setStockKg(supplier.getStockKg().subtract(order.getQuantityKg()).setScale(2, RoundingMode.HALF_UP));
        supplierRepository.save(supplier);

        order.setSelectedSupplierId(supplier.getId());
        order.setSelectedSupplierName(supplier.getSupplierName());
        order.setSelectedPurchasePrice(purchasePrice);
        order.setSalePrice(salePrice);
        order.setCostAmount(cost);
        order.setOrderAmount(amount);
        order.setProfitAmount(amount.subtract(cost).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.SUPPLIER_SELECTED);
    }

    private void restoreStock(CustomerOrder order) {
        if (order.getSelectedSupplierId() == null || order.getStatus() != OrderStatus.SUPPLIER_SELECTED) {
            return;
        }
        supplierRepository.findById(order.getSelectedSupplierId()).ifPresent(supplier -> {
            supplier.setStockKg(supplier.getStockKg().add(order.getQuantityKg()).setScale(2, RoundingMode.HALF_UP));
            supplierRepository.save(supplier);
        });
    }

    private void clearCalculatedFields(CustomerOrder order) {
        order.setSelectedSupplierId(null);
        order.setSelectedSupplierName(null);
        order.setSelectedPurchasePrice(null);
        order.setSalePrice(null);
        order.setOrderAmount(null);
        order.setCostAmount(null);
        order.setProfitAmount(null);
        order.setStatus(OrderStatus.MANUAL_REVIEW);
    }
}
