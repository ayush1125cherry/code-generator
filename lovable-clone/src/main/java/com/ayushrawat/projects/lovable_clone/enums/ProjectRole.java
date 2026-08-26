package com.ayushrawat.projects.lovable_clone.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.ayushrawat.projects.lovable_clone.enums.ProjectPermission.*;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum ProjectRole {

    //One Way
    EDITOR(EDIT),
    VIEWER(Set.of(VIEW,VIEW_MEMBERS)),
    OWNER(Set.of(EDIT,VIEW,DELETE,MANAGE_MEMBERS,VIEW_MEMBERS));

    //Another Way
    ProjectRole(ProjectPermission... permission) {
        this.permission = Set.of(permission);
    }

    private final Set<ProjectPermission> permission;
}
