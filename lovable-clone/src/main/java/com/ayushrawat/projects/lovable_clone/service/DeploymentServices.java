package com.ayushrawat.projects.lovable_clone.service;

import com.ayushrawat.projects.lovable_clone.dto.deploy.DeployResponse;

public interface DeploymentServices {
    DeployResponse deploy(Long projectId);

}
