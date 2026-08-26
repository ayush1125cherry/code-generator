package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.dto.member.InviteMemberRequest;
import com.ayushrawat.projects.lovable_clone.dto.member.MemberResponse;
import com.ayushrawat.projects.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.ayushrawat.projects.lovable_clone.entity.Project;
import com.ayushrawat.projects.lovable_clone.entity.ProjectMember;
import com.ayushrawat.projects.lovable_clone.entity.ProjectMemberId;
import com.ayushrawat.projects.lovable_clone.entity.User;
import com.ayushrawat.projects.lovable_clone.mapper.ProjectMemberMapper;
import com.ayushrawat.projects.lovable_clone.repository.ProjectMemberRepository;
import com.ayushrawat.projects.lovable_clone.repository.ProjectRepository;
import com.ayushrawat.projects.lovable_clone.repository.UserRepository;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    UserRepository userRepository;
    AuthUtils authUtils;


    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {


        return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .toList();

    }

    @Override
    @PreAuthorize("@security.canManageMember(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtils.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId,userId);

//        User invitee = userRepository.findByUsername(request.username()).orElseThrow();
        System.out.println("Searching username = " + request.username());

        User invitee = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + request.username()
                ));

        if(invitee.getId().equals(userId)){
            throw new RuntimeException("Not allowed to invite");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.getId());
        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("cannot invite once again");
        }

        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .user(invitee)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();

        projectMemberRepository.save(member);

        return projectMemberMapper.toProjectMemberResponseFromMember(member);
    }

    @Override
    @PreAuthorize("@security.canManageMember(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId = authUtils.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId,userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember= projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());

        projectMemberRepository.save(projectMember);


        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageMember(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId = authUtils.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId,userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Project Member not found in project");
        }

        projectMemberRepository.deleteById(projectMemberId);

    }


    //Internal functions...
    public Project getAccessibleProjectById(Long projectId,Long userId){
        return  projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();

    }
}
