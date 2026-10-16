package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.entity.Project;
import com.ayushrawat.projects.lovable_clone.entity.ProjectFile;
import com.ayushrawat.projects.lovable_clone.error.ResourceNotFoundException;
import com.ayushrawat.projects.lovable_clone.repository.ProjectFileRepository;
import com.ayushrawat.projects.lovable_clone.repository.ProjectRepository;
import com.ayushrawat.projects.lovable_clone.service.ProjectTemplateService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final MinioClient minioClient;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    private static final String TARGET_BUCKET = "projects";

    private static final List<String> STARTER_FILES = List.of(
            "eslint.config.js",
            "index.html",
            "package.json",
            "tsconfig.json",
            "vite.config.js",
            "public/daisyui.svg",
            "public/vite.svg",
            "src/App.css",
            "src/App.tsx",
            "src/index.css",
            "src/main.tsx",
            "src/vite-env.d.ts",
            "src/assets/react.svg",
            "src/pages/Index.tsx",
            "src/pages/NotFound.tsx"
    );

    @Override
    public void initializerProjectFromTemplate(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString()));

        // 1. Always create the project file metadata in PostgreSQL first
        List<ProjectFile> filesToSave = new ArrayList<>();
        for (String relPath : STARTER_FILES) {
            String destKey = projectId + "/" + relPath;
            ProjectFile pf = ProjectFile.builder()
                    .project(project)
                    .path(relPath)
                    .minioObjectKey(destKey)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            filesToSave.add(pf);
        }
        projectFileRepository.saveAll(filesToSave);
        log.info("Initialized {} starter files in Postgres for projectId: {}", filesToSave.size(), projectId);

        // 2. Best-effort upload to MinIO. If MinIO is cold/sleeping on Render, log warning and do not fail project creation!
        seedFilesToMinio(projectId);
    }

    private void seedFilesToMinio(Long projectId) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(TARGET_BUCKET).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(TARGET_BUCKET).build());
                log.info("Auto-created missing MinIO bucket: {}", TARGET_BUCKET);
            }

            for (String relPath : STARTER_FILES) {
                String destKey = projectId + "/" + relPath;
                try {
                    ClassPathResource resource = new ClassPathResource("starter-template/" + relPath);
                    if (resource.exists()) {
                        byte[] bytes;
                        try (InputStream is = resource.getInputStream()) {
                            bytes = is.readAllBytes();
                        }
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(TARGET_BUCKET)
                                        .object(destKey)
                                        .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                                        .contentType(determineContentType(relPath))
                                        .build()
                        );
                    }
                } catch (Exception ex) {
                    log.warn("Could not copy starter template file {} to MinIO for project {}: {}", relPath, projectId, ex.getMessage());
                }
            }
            log.info("Successfully seeded starter files to MinIO for project {}", projectId);
        } catch (Exception e) {
            log.warn("MinIO seed skipped for project {} (MinIO may be sleeping or restarting): {}", projectId, e.getMessage());
        }
    }

    private String determineContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        if (type != null) return type;
        if (path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx")) return "text/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}





















