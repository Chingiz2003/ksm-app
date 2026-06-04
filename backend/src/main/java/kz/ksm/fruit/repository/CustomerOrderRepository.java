package kz.ksm.fruit.repository;

import kz.ksm.fruit.model.CustomerOrder;
import kz.ksm.fruit.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByManagerUsernameOrderByIdAsc(String managerUsername);

    List<CustomerOrder> findByStatus(OrderStatus status);
}
