package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.dto.project.ProjectRequest;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectResponse;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectSummaryResponse;
import com.ayushrawat.projects.lovable_clone.entity.Project;
import com.ayushrawat.projects.lovable_clone.entity.ProjectMember;
import com.ayushrawat.projects.lovable_clone.entity.ProjectMemberId;
import com.ayushrawat.projects.lovable_clone.entity.User;
import com.ayushrawat.projects.lovable_clone.enums.ProjectRole;
import com.ayushrawat.projects.lovable_clone.error.BadRequestException;
import com.ayushrawat.projects.lovable_clone.error.ResourceNotFoundException;
import com.ayushrawat.projects.lovable_clone.mapper.ProjectMapper;
import com.ayushrawat.projects.lovable_clone.repository.ProjectMemberRepository;
import com.ayushrawat.projects.lovable_clone.repository.ProjectRepository;
import com.ayushrawat.projects.lovable_clone.repository.UserRepository;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.ProjectService;
import com.ayushrawat.projects.lovable_clone.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtils authUtils;
    SubscriptionService subscriptionService;
    ProjectTemplateServiceImpl projectTemplateService;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        if(!subscriptionService.canCreateNewProject()){
            throw new BadRequestException("User cannot create new project with current plan ,upgrade your plan");
        }
        Long userId = authUtils.getCurrentUserId();

//        User owner = userRepository.findById(userId).orElseThrow(
//                ()-> new ResourceNotFoundException("User",userId.toString()));

        User owner = userRepository.getReferenceById(userId);

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        project = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), owner.getId());
        ProjectMember projectMember=ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .user(owner)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();

        projectMemberRepository.save(projectMember);

        projectTemplateService.initializerProjectFromTemplate(project.getId());



        return projectMapper.toProjectResponse(project);
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtils.getCurrentUserId();

        var projectsWithRoles = projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRoles.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(), p.getRole()))
                .toList();

    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtils.getCurrentUserId();
        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project Not Found"));

        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole());
    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId = authUtils.getCurrentUserId();
        Project project =getAccessibleProjectById(projectId,userId);

        project.setName(request.name());
        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);

    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public void softDelete(Long projectId) {
        Long userId = authUtils.getCurrentUserId();
        Project project =getAccessibleProjectById(projectId,userId);

        project.setDeletedAt(Instant.now());
        projectRepository.save(project);

    }

    //Internal functions...
    public Project getAccessibleProjectById(Long projectId,Long userId){
        return  projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow(()->new ResourceNotFoundException("Project",projectId.toString()));


    }
}
