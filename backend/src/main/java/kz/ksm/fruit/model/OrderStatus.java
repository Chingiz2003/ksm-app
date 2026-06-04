package kz.ksm.fruit.model;

public enum OrderStatus {
    SUPPLIER_SELECTED("Подобран поставщик"),
    MANUAL_REVIEW("Требует ручного разбора");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
