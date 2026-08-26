package com.ayushrawat.projects.lovable_clone.service;


public interface UsageService {
    void        recordTokenUsage(Long userId, int actualTokens);
    void checkDailyTokensUsage();
}

