package com.mejbri.pfe.netopssynchro.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAnalysisResultDTO {
    private Long serverId;
    private String serverName;
    private boolean hasErrors;
    private String errorSummary;
    private List<String> affectedServices;
    private String rootCauseSuggestion;
    private String recommendedAction;
    private String rawLogSnippet;
}
