package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.dto.chat.StreamResponse;
import com.ayushrawat.projects.lovable_clone.entity.*;
import com.ayushrawat.projects.lovable_clone.enums.ChatEventType;
import com.ayushrawat.projects.lovable_clone.enums.MessageRole;
import com.ayushrawat.projects.lovable_clone.error.ResourceNotFoundException;
import com.ayushrawat.projects.lovable_clone.llm.LlmResponseParser;
import com.ayushrawat.projects.lovable_clone.llm.PromptUtils;
import com.ayushrawat.projects.lovable_clone.llm.advisor.FileTreeContextAdvisor;
import com.ayushrawat.projects.lovable_clone.llm.tools.CodeGenerationTool;
import com.ayushrawat.projects.lovable_clone.repository.*;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.AIGenerationService;
import com.ayushrawat.projects.lovable_clone.service.ProjectFileService;
import com.ayushrawat.projects.lovable_clone.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;


@RequiredArgsConstructor
@Slf4j
@Service
public class AiGenerationServiceImpl implements AIGenerationService {


    private static final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);


    private final ChatClient chatClient;
    private final AuthUtils authUtils;
    private final ProjectFileService projectFileService;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
    private final ProjectRepository projectRepository;
    private final LlmResponseParser llmResponseParser;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;


    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {
//        usageService.checkDailyTokensUsage();
        Long userId = authUtils.getCurrentUserId();

        ChatSession chatSession = createChatSessionIfNotExists(projectId,userId);

        Map<String,Object> advisorParams = Map.of(
                "userId",userId,
                "projectId",projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();

        CodeGenerationTool codeGenerationTool = new CodeGenerationTool(projectFileService,projectId);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();



        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTool)
                .advisors(advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor );
                        }
                )
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String content = response.getResult().getOutput().getText();
                    if(content != null && !content.isEmpty() && endTime.get() == 0) { // first non-empty chunk received
                        endTime.set(System.currentTimeMillis());
                    }
                    if(response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    fullResponseBuffer.append(content);
                })


                .doOnComplete(()->{
                    Schedulers.boundedElastic().schedule(()->{
//                        parseAndSaveFiles(fullResponseBuffer.toString(), projectId);
                        long duration = (endTime.get() - startTime.get()) /  1000;

                        finalizeChats(userMessage,chatSession,fullResponseBuffer.toString(),duration,usageRef.get(),userId);
                    });

                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId))
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    return new StreamResponse(text != null ? text : "");
                });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration,Usage usage,Long userId) {
        Long projectId = chatSession.getProject().getId();

        if(usage != null) {
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getUser().getId(), totalTokens);
        }

        // Save the User message
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(usage.getPromptTokens())
                        .build());



        ChatMessage assistantChatMessage = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .content(fullText)
                .chatSession(chatSession)
                .tokensUsed(usage.getCompletionTokens())
                .build();

        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .chatMessage(assistantChatMessage)
                .content("Thought for "+duration+"s")
                .sequenceOrder(0)
                .build());

        chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .forEach(e -> projectFileService.saveFile(projectId, e.getFilePath(), e.getContent()));

        chatEventRepository.saveAll(chatEventList);
    }

        private ChatSession createChatSessionIfNotExists(Long projectId, Long userId){
        ChatSessionId chatSessionId = new ChatSessionId(projectId,userId);

            ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

            System.out.println("Found = " + chatSession);

            if (chatSession == null) {

                Project project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

                chatSession = ChatSession.builder()
                        .id(chatSessionId)
                        .project(project)
                        .user(user)
                        .build();

                chatSession = chatSessionRepository.save(chatSession);



                System.out.println("Saved ChatSession: " + chatSession);

                System.out.println("Saved!");

                System.out.println(
                        chatSessionRepository.findById(chatSessionId));

            }

            return chatSession;
    }
}
