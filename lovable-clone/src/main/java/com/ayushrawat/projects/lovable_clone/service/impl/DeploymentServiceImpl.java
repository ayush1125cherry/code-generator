package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.dto.deploy.DeployResponse;
import com.ayushrawat.projects.lovable_clone.service.DeploymentServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeploymentServiceImpl implements DeploymentServices {

    @Value("${client.url:http://localhost:3000}")
    private String clientUrl;

    @Override
    public DeployResponse deploy(Long projectId) {
        String previewUrl = clientUrl + "/preview/" + projectId;
        log.info("Generating preview URL for project {}: {}", projectId, previewUrl);
        return new DeployResponse(previewUrl);
    }
}
