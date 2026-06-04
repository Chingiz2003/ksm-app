package kz.ksm.fruit.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceSummaryDto(
        BigDecimal totalRevenue,
        BigDecimal totalProfit,
        List<ProfitRowDto> profitByFruit,
        List<ProfitRowDto> profitByClient,
        List<ProfitRowDto> revenueByDay
) {
}
