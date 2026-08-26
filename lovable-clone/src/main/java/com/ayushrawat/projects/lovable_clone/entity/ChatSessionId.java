package com.ayushrawat.projects.lovable_clone.entity;


import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ChatSessionId implements Serializable {
    Long projectId;
    Long userId;
}
