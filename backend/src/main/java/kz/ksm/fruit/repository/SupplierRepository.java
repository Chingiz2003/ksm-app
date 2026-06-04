package kz.ksm.fruit.repository;

import kz.ksm.fruit.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByFruitNameIgnoreCase(String fruitName);
}
