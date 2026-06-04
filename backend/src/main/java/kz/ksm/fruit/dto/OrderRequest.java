package kz.ksm.fruit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderRequest(
        @NotBlank String restaurant,
        @NotBlank String fruitName,
        @NotNull @DecimalMin("0.0") BigDecimal quantityKg,
        @NotNull LocalDate orderDate,
        @NotNull LocalDate deliveryDate
) {
}
