package kz.ksm.fruit.controller;

import jakarta.validation.Valid;
import kz.ksm.fruit.model.AuditAction;
import kz.ksm.fruit.model.Supplier;
import kz.ksm.fruit.repository.SupplierRepository;
import kz.ksm.fruit.service.AuditService;
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

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public SupplierController(SupplierRepository supplierRepository, AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Supplier> findAll() {
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getId))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER','MANAGER')")
    public Supplier create(@Valid @RequestBody Supplier supplier, Authentication authentication) {
        supplier.setId(null);
        Supplier saved = supplierRepository.save(supplier);
        auditService.record(authentication, AuditAction.CREATE, "Поставщик", saved.getId(), saved.getSupplierName(),
                "Добавлен поставщик " + saved.getSupplierName() + " для фрукта " + saved.getFruitName());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER','MANAGER')")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody Supplier request, Authentication authentication) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        supplier.setFruitName(request.getFruitName());
        supplier.setPurchasePrice(request.getPurchasePrice());
        supplier.setStockKg(request.getStockKg());
        supplier.setSupplierName(request.getSupplierName());
        supplier.setDeliveryDays(request.getDeliveryDays());
        Supplier saved = supplierRepository.save(supplier);
        auditService.record(authentication, AuditAction.UPDATE, "Поставщик", saved.getId(), saved.getSupplierName(),
                "Обновлены данные поставщика " + saved.getSupplierName() + " для фрукта " + saved.getFruitName());
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER','MANAGER')")
    public void delete(@PathVariable Long id, Authentication authentication) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        supplierRepository.delete(supplier);
        auditService.record(authentication, AuditAction.DELETE, "Поставщик", id, supplier.getSupplierName(),
                "Удален поставщик " + supplier.getSupplierName() + " для фрукта " + supplier.getFruitName());
    }
}
