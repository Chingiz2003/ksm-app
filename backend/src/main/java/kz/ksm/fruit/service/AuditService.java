package kz.ksm.fruit.service;

import kz.ksm.fruit.model.AuditAction;
import kz.ksm.fruit.model.AuditLog;
import kz.ksm.fruit.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest() {
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void record(Authentication authentication, AuditAction action, String entityType, Long entityId, String entityName, String details) {
        AuditLog log = new AuditLog();
        log.setCreatedAt(Instant.now());
        log.setUsername(authentication.getName());
        log.setRoleLabel(roleLabel(authentication));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setEntityName(entityName);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String roleLabel(Authentication authentication) {
        if (hasRole(authentication, "ROLE_MANAGER")) {
            return "Руководитель";
        }
        if (hasRole(authentication, "ROLE_SALES")) {
            return "Менеджер продаж";
        }
        return "Закупщик";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
