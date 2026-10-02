package com.ayushrawat.projects.lovable_clone.config;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
@RequiredArgsConstructor
public class MinioInitializer implements ApplicationRunner {

    private final MinioClient minioClient;

    @Value("${minio.project-bucket:projects}")
    private String projectBucket;

    public static final String TEMPLATE_BUCKET = "starter-project";
    public static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter";

    @Override
    public void run(ApplicationArguments args) {
        ensureBucketsAndTemplates();
    }

    public synchronized void ensureBucketsAndTemplates() {
        try {
            String targetBucket = (projectBucket != null && !projectBucket.isBlank()) ? projectBucket : "projects";

            // 1. Ensure 'projects' bucket exists
            boolean targetExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(targetBucket).build());
            if (!targetExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
                log.info("Auto-created missing MinIO bucket: {}", targetBucket);
            }

            // 2. Ensure 'starter-project' bucket exists
            boolean templateBucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(TEMPLATE_BUCKET).build());
            if (!templateBucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(TEMPLATE_BUCKET).build());
                log.info("Auto-created missing MinIO bucket: {}", TEMPLATE_BUCKET);
            }

            // 3. Ensure template files exist in 'starter-project'
            boolean templateReady = false;
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(TEMPLATE_BUCKET)
                                .object(TEMPLATE_NAME + "/package.json")
                                .build()
                );
                templateReady = true;
            } catch (Exception ex) {
                templateReady = false;
            }

            if (!templateReady) {
                log.info("Starter template missing in MinIO. Auto-seeding from classpath...");
                seedTemplateFromClasspath();
            }

        } catch (Exception e) {
            log.warn("MinIO auto-initialization warning (MinIO might still be starting): {}", e.getMessage());
        }
    }

    private void seedTemplateFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:starter-template/**");
            int count = 0;

            for (Resource resource : resources) {
                if (!resource.isReadable()) continue;

                String uri = resource.getURI().toString();
                int idx = uri.indexOf("starter-template/");
                if (idx == -1) continue;

                String relPath = uri.substring(idx + "starter-template/".length());
                if (relPath.isBlank() || relPath.endsWith("/")) continue;

                String objectKey = TEMPLATE_NAME + "/" + relPath;

                try (InputStream is = resource.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(TEMPLATE_BUCKET)
                                    .object(objectKey)
                                    .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                                    .build()
                    );
                    count++;
                }
            }
            log.info("Successfully auto-seeded {} template files into MinIO bucket '{}'", count, TEMPLATE_BUCKET);
        } catch (Exception e) {
            log.error("Failed to auto-seed template files to MinIO: {}", e.getMessage(), e);
        }
    }
}
