package com.ayushrawat.projects.lovable_clone.service;

import com.ayushrawat.projects.lovable_clone.dto.project.FileContentResponse;
import com.ayushrawat.projects.lovable_clone.dto.project.FileNode;
import com.ayushrawat.projects.lovable_clone.dto.project.FileTreeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProjectFileService {

    FileTreeResponse getFileTree(Long projectId);


    FileContentResponse getFileContent(Long projectId, String path);


    void saveFile(Long projectId, String filePath, String fileContent);
}
