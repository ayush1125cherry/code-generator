package com.ayushrawat.projects.lovable_clone.service;


import com.ayushrawat.projects.lovable_clone.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;


public interface AIGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long aLong);
}
