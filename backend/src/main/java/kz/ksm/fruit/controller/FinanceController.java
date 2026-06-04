package kz.ksm.fruit.controller;

import kz.ksm.fruit.dto.FinanceSummaryDto;
import kz.ksm.fruit.service.FinanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SALES','MANAGER')")
    public FinanceSummaryDto summary(Authentication authentication) {
        return financeService.summarize(authentication);
    }
}
