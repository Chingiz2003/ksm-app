package kz.ksm.fruit.controller;

import kz.ksm.fruit.dto.AuthUserDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public AuthUserDto me(Authentication authentication) {
        Set<String> authorities = new LinkedHashSet<>();
        authentication.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));

        if (authorities.contains("ROLE_MANAGER")) {
            return new AuthUserDto(authentication.getName(), "MANAGER", "Руководитель",
                    Set.of("suppliers:read", "suppliers:write", "orders:read", "orders:write", "finance:company", "audit:read"));
        }
        if (authorities.contains("ROLE_SALES")) {
            return new AuthUserDto(authentication.getName(), "SALES", "Менеджер продаж",
                    Set.of("suppliers:read", "orders:read", "orders:write", "finance:own"));
        }
        return new AuthUserDto(authentication.getName(), "BUYER", "Закупщик",
                Set.of("suppliers:read", "suppliers:write", "orders:read"));
    }
}
