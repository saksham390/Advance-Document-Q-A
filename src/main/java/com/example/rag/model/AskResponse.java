package com.example.rag.model;

import java.util.List;

public record AskResponse(String answer, String rewrittenQuery, List<Source> sources) {
}
