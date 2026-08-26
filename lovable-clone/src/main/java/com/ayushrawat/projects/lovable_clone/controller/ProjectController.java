package com.ayushrawat.projects.lovable_clone.controller;

import com.ayushrawat.projects.lovable_clone.dto.deploy.DeployResponse;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectRequest;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectResponse;
import com.ayushrawat.projects.lovable_clone.dto.project.ProjectSummaryResponse;
import com.ayushrawat.projects.lovable_clone.service.DeploymentServices;
import com.ayushrawat.projects.lovable_clone.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DeploymentServices deploymentServices;

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects() {
        System.out.println("Controller called");

        List<ProjectSummaryResponse> projects = projectService.getUserProjects();

        System.out.println("Projects = " + projects);
        System.out.println("Size = " + (projects == null ? "null" : projects.size()));

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectSummaryResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getUserProjectById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long id){

        return ResponseEntity.ok(deploymentServices.deploy(id));
    }

}

















