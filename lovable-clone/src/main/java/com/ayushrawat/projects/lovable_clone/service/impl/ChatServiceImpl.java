package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.entity.ChatMessage;
import com.ayushrawat.projects.lovable_clone.entity.ChatSession;
import com.ayushrawat.projects.lovable_clone.entity.ChatSessionId;
import com.ayushrawat.projects.lovable_clone.mapper.ChatMapper;
import com.ayushrawat.projects.lovable_clone.repository.ChatMessageRepository;
import com.ayushrawat.projects.lovable_clone.repository.ChatSessionRepository;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ayushrawat.projects.lovable_clone.dto.chat.ChatResponse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AuthUtils authUtils;
    private final ChatMapper chatMapper;
    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {
        Long userId = authUtils.getCurrentUserId();

        return chatSessionRepository.findById(new ChatSessionId(projectId, userId))
                .map(chatSession -> {
                    List<ChatMessage> chatMessageList = chatMessageRepository.findByChatSession(chatSession);
                    log.info("Messages found = {}", chatMessageList.size());
                    List<ChatResponse> responses = chatMapper.fromListOfChatMessages(chatMessageList);
                    log.info("Mapped responses = {}", responses);
                    return responses;
                })
                .orElseGet(List::of);
    }
}
