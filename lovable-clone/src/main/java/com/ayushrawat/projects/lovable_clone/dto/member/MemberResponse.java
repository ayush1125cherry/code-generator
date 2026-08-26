package com.ayushrawat.projects.lovable_clone.dto.member;

import com.ayushrawat.projects.lovable_clone.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String username,
        String name,
        String avatarUrl,
        ProjectRole role,
        Instant invitedAt
) {
}
