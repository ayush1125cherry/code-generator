package com.ayushrawat.projects.lovable_clone.repository;

import com.ayushrawat.projects.lovable_clone.entity.ChatEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatEventRepository extends JpaRepository<ChatEvent,Long> {
}
