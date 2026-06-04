package kz.ksm.fruit.dto;

import java.util.Set;

public record AuthUserDto(
        String username,
        String role,
        String roleLabel,
        Set<String> permissions
) {
}
