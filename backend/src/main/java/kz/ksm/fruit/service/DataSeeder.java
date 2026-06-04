package kz.ksm.fruit.service;

import kz.ksm.fruit.dto.OrderRequest;
import kz.ksm.fruit.model.Supplier;
import kz.ksm.fruit.repository.CustomerOrderRepository;
import kz.ksm.fruit.repository.SupplierRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DataSeeder implements ApplicationRunner {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SupplierRepository supplierRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderService orderService;
    private final String suppliersFile;
    private final String ordersFile;

    public DataSeeder(
            SupplierRepository supplierRepository,
            CustomerOrderRepository orderRepository,
            OrderService orderService,
            @Value("${app.seed.suppliers-file}") String suppliersFile,
            @Value("${app.seed.orders-file}") String ordersFile
    ) {
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.suppliersFile = suppliersFile;
        this.ordersFile = ordersFile;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (supplierRepository.count() > 0 || orderRepository.count() > 0) {
            return;
        }
        seedSuppliers(resolve(suppliersFile));
        seedOrders(resolve(ordersFile));
    }

    private void seedSuppliers(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || text(row.getCell(1)).isBlank()) {
                    continue;
                }
                Supplier supplier = new Supplier();
                supplier.setFruitName(text(row.getCell(1)));
                supplier.setPurchasePrice(number(row.getCell(2)));
                supplier.setStockKg(number(row.getCell(3)));
                supplier.setSupplierName(text(row.getCell(4)));
                supplier.setDeliveryDays(number(row.getCell(5)).intValue());
                supplierRepository.save(supplier);
            }
        }
    }

    private void seedOrders(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || text(row.getCell(1)).isBlank()) {
                    continue;
                }
                OrderRequest request = new OrderRequest(
                        text(row.getCell(1)),
                        text(row.getCell(2)),
                        number(row.getCell(3)),
                        date(row.getCell(6)),
                        date(row.getCell(7))
                );
                orderService.create(request, "sales");
            }
        }
    }

    private Path resolve(String configured) {
        Path path = Path.of(configured);
        if (Files.exists(path)) {
            return path;
        }
        Path fromRoot = Path.of("data").resolve(path.getFileName());
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        throw new IllegalStateException("Seed file not found: " + configured);
    }

    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private BigDecimal number(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType().name().equals("NUMERIC")) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String value = text(cell).replace(" ", "").replace(",", ".");
        return value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private LocalDate date(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Date cell is empty");
        }
        if (cell.getCellType().name().equals("NUMERIC") && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(text(cell), DATE_FORMAT);
    }
}
