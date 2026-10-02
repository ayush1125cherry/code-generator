package com.ayushrawat.projects.lovable_clone.llm;

import com.ayushrawat.projects.lovable_clone.entity.ChatEvent;
import com.ayushrawat.projects.lovable_clone.entity.ChatMessage;
import com.ayushrawat.projects.lovable_clone.enums.ChatEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LlmResponseParser {
    /**
     * Regex Breakdown:
     * Group 1: Opening Tag (<tag ...>)
     * Group 2: Tag Name (message|file|tool)
     * Group 3: Attributes part (e.g., ' path="foo"' or ' args="a,b"')
     * Group 4: Content (The stuff inside)
     * Group 5: Closing Tag (</tag>)
     */

    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile(
            "(<(message|file|tool)([^>]*)>)([\\s\\S]*?)(</\\2>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Helper to extract specific attributes (path="..." or args="...") supporting single or double quotes
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(path|args)=[\"']([^\"']+)[\"']"
    );

    private static final Pattern UNCLOSED_FILE_PATTERN = Pattern.compile(
            "<file[^>]*path=[\"']([^\"']+)[\"'][^>]*>([\\s\\S]+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MARKDOWN_CODE_PATTERN = Pattern.compile(
            "```(?:tsx|jsx|typescript|javascript|html)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE
    );

    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentMessage) {
        List<ChatEvent> events = new ArrayList<>();
        int orderCounter = 1;

        if (fullResponse == null || fullResponse.isBlank()) {
            return events;
        }

        Matcher matcher = GENERIC_TAG_PATTERN.matcher(fullResponse);

        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            String content = matcher.group(4).trim();

            if ("file".equals(tagName)) {
                content = cleanFileContent(content);
            }

            // Extract attributes map
            Map<String, String> attrMap = extractAttributes(attributes);

            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .chatMessage(parentMessage)
                    .content(content)
                    .sequenceOrder(orderCounter++);

            switch (tagName) {
                case "message" -> builder.type(ChatEventType.MESSAGE);
                case "file" -> {
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.filePath(attrMap.get("path"));
                }
                case "tool" -> {
                    builder.type(ChatEventType.TOOL_LOG);
                    builder.metadata(attrMap.get("args"));
                }
                default -> { continue; }
            }

            events.add(builder.build());
        }

        boolean hasFileEdit = events.stream().anyMatch(e -> e.getType() == ChatEventType.FILE_EDIT && e.getFilePath() != null && !e.getFilePath().isBlank());

        // Fallback 1: Check for unclosed <file ...> tag if generation truncated
        if (!hasFileEdit) {
            Matcher unclosedMatcher = UNCLOSED_FILE_PATTERN.matcher(fullResponse);
            if (unclosedMatcher.find()) {
                String path = unclosedMatcher.group(1);
                String content = cleanFileContent(unclosedMatcher.group(2));
                if (path != null && !path.isBlank() && !content.isBlank()) {
                    log.info("Recovered unclosed <file> tag for path: {}", path);
                    events.add(ChatEvent.builder()
                            .chatMessage(parentMessage)
                            .type(ChatEventType.FILE_EDIT)
                            .filePath(path)
                            .content(content)
                            .sequenceOrder(orderCounter++)
                            .build());
                    hasFileEdit = true;
                }
            }
        }

        // Fallback 2: Check for markdown code blocks containing React component code
        if (!hasFileEdit) {
            Matcher codeMatcher = MARKDOWN_CODE_PATTERN.matcher(fullResponse);
            while (codeMatcher.find()) {
                String code = codeMatcher.group(1).trim();
                if (code.contains("export default") || code.contains("React") || code.contains("return (") || code.contains("return <")) {
                    log.info("Recovered markdown code block as src/pages/Index.tsx");
                    events.add(ChatEvent.builder()
                            .chatMessage(parentMessage)
                            .type(ChatEventType.FILE_EDIT)
                            .filePath("src/pages/Index.tsx")
                            .content(code)
                            .sequenceOrder(orderCounter++)
                            .build());
                    break;
                }
            }
        }

        return events;
    }

    private String cleanFileContent(String raw) {
        if (raw == null) return "";
        String clean = raw.trim();
        // Remove CDATA
        if (clean.startsWith("<![CDATA[")) {
            clean = clean.substring(9).trim();
        }
        if (clean.endsWith("]]>")) {
            clean = clean.substring(0, clean.length() - 3).trim();
        }
        clean = clean.replaceAll("<!\\[CDATA\\[|\\]\\]>", "").trim();
        // Remove markdown code fences
        clean = clean.replaceAll("^```[a-zA-Z0-9_-]*\\s*", "").trim();
        clean = clean.replaceAll("\\s*```$", "").trim();
        return clean;
    }

    private Map<String, String> extractAttributes(String attributeString) {
        Map<String, String> attributes = new HashMap<>();
        if (attributeString == null) return attributes;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributeString);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2));
        }
        return attributes;
    }

}
