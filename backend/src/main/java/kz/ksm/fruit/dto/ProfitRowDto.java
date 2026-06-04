package kz.ksm.fruit.dto;

import java.math.BigDecimal;

public record ProfitRowDto(
        String name,
        BigDecimal revenue,
        BigDecimal profit
) {
}
