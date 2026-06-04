package kz.ksm.fruit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String restaurant;

    @NotBlank
    @Column(nullable = false)
    private String fruitName;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal orderAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal costAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal profitAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDate orderDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate deliveryDate;

    private Long selectedSupplierId;

    private String selectedSupplierName;

    @Column(precision = 12, scale = 2)
    private BigDecimal selectedPurchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.MANUAL_REVIEW;

    @Column(nullable = false)
    private String managerUsername = "sales";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public BigDecimal getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }

    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    public void setProfitAmount(BigDecimal profitAmount) {
        this.profitAmount = profitAmount;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Long getSelectedSupplierId() {
        return selectedSupplierId;
    }

    public void setSelectedSupplierId(Long selectedSupplierId) {
        this.selectedSupplierId = selectedSupplierId;
    }

    public String getSelectedSupplierName() {
        return selectedSupplierName;
    }

    public void setSelectedSupplierName(String selectedSupplierName) {
        this.selectedSupplierName = selectedSupplierName;
    }

    public BigDecimal getSelectedPurchasePrice() {
        return selectedPurchasePrice;
    }

    public void setSelectedPurchasePrice(BigDecimal selectedPurchasePrice) {
        this.selectedPurchasePrice = selectedPurchasePrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getManagerUsername() {
        return managerUsername;
    }

    public void setManagerUsername(String managerUsername) {
        this.managerUsername = managerUsername;
    }
}
