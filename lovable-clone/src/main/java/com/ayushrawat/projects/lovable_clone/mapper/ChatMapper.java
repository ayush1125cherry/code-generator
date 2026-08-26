package com.ayushrawat.projects.lovable_clone.mapper;

import com.ayushrawat.projects.lovable_clone.entity.ChatMessage;
import org.mapstruct.Mapper;
import com.ayushrawat.projects.lovable_clone.dto.chat.ChatResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {
    List<ChatResponse> fromListOfChatMessages(List<ChatMessage> chatMessageList);
}
