package com.example.invoicetracker.dto.ai;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    public static GeminiRequest createTextRequest(String prompt) {
        return GeminiRequest.builder()
                .contents(List.of(
                    Content.builder()
                        .parts(List.of(
                            Part.builder()
                                .text(prompt)
                                .build()
                        ))
                        .build()
                ))
                .build();
    }
}



