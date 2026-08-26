package com.ayushrawat.projects.lovable_clone.dto.member;

import com.ayushrawat.projects.lovable_clone.enums.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotNull @NotBlank String username,
        @NotNull ProjectRole role
) {
}
