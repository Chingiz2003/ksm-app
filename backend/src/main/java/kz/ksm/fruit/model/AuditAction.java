package kz.ksm.fruit.model;

public enum AuditAction {
    CREATE("Создание"),
    UPDATE("Редактирование"),
    DELETE("Удаление");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
