package com.ayushrawat.projects.lovable_clone.service;

import com.ayushrawat.projects.lovable_clone.dto.project.ProjectRequest;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectResponse;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects();

    ProjectSummaryResponse getUserProjectById(Long id);

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(Long id, ProjectRequest request);

    void softDelete(Long id);
}
