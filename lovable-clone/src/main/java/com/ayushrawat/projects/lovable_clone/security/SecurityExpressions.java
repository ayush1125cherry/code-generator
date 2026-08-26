package com.ayushrawat.projects.lovable_clone.security;


import com.ayushrawat.projects.lovable_clone.enums.ProjectPermission;
import com.ayushrawat.projects.lovable_clone.repository.ProjectMemberRepository;
import com.ayushrawat.projects.lovable_clone.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor

public class SecurityExpressions {
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthUtils authUtils;
    private final UserRepository userRepository;

    private boolean hasPermission(Long projectId,ProjectPermission projectPermission){
        Long userId = authUtils.getCurrentUserId();

        System.out.println("UserId = " + userId);
        System.out.println("ProjectId = " + projectId);

        var role = projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId);

        System.out.println("Role = " + role);

        return role
                .map(r -> r.getPermission().contains(projectPermission))
                .orElse(false);
    }

    public boolean canViewProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.VIEW);
    }



    public boolean canEditProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.EDIT);
    }

    public boolean canDeleteProject(Long projectId){
        return hasPermission(projectId, ProjectPermission.DELETE);
    }

    public boolean canViewMembers(Long projectId){
        return hasPermission(projectId, ProjectPermission.VIEW_MEMBERS);
    }
    public boolean canManageMember(Long projectId){
        return hasPermission(projectId, ProjectPermission.MANAGE_MEMBERS);
    }
}
