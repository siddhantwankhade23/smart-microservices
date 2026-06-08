package com.upskill.smart.dto;

import com.upskill.smart.Role;

public record LoginRequest(String username, String password, Role role) {
}
