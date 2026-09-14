package com.ayushrawat.projects.lovable_clone.controller;

import com.ayushrawat.projects.lovable_clone.dto.project.FileNode;
import com.ayushrawat.projects.lovable_clone.service.ProjectFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/preview")
@RequiredArgsConstructor
@Slf4j
public class PreviewController {

    private final ProjectFileService projectFileService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String htmlTemplate = "";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("preview-template.html");
            try (InputStream is = resource.getInputStream()) {
                htmlTemplate = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to load preview-template.html from classpath", e);
        }
    }

    @GetMapping(value = "/{projectId:[0-9]+}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> renderPreview(@PathVariable Long projectId) {
        try {
            List<FileNode> fileNodes = projectFileService.getFileTree(projectId).files();
            Map<String, String> filesMap = new HashMap<>();
            collectFiles(projectId, fileNodes, filesMap);

            String filesJson = objectMapper.writeValueAsString(filesMap);
            String filesBase64 = java.util.Base64.getEncoder().encodeToString(filesJson.getBytes(StandardCharsets.UTF_8));
            String renderedHtml = htmlTemplate.replace("__FILES_BASE64__", filesBase64);

            return ResponseEntity.ok(renderedHtml);
        } catch (Exception e) {
            log.error("Failed to render preview for projectId: {}", projectId, e);
            return ResponseEntity.ok(generateErrorHtml("Failed to load preview: " + e.getMessage()));
        }
    }

    private void collectFiles(Long projectId, List<FileNode> nodes, Map<String, String> filesMap) {
        if (nodes == null) return;
        for (FileNode node : nodes) {
            if (node == null || node.path() == null) continue;
            try {
                String content = projectFileService.getFileContent(projectId, node.path()).content();
                filesMap.put(node.path(), content);
            } catch (Exception e) {
                log.warn("Could not read file {} for project {}: {}", node.path(), projectId, e.getMessage());
            }
        }
    }

    private String generateErrorHtml(String message) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="background:#0f172a;color:#94a3b8;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;">
              <div style="text-align:center;padding:24px;">
                <h3 style="color:#f87171;margin-bottom:8px;">Preview Unavailable</h3>
                <p>""" + message + """
                </p>
              </div>
            </body>
            </html>
            """;
    }
}
