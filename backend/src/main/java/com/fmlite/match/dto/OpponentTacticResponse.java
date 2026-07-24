package com.fmlite.match.dto;

import java.util.List;

/** 경기 중 상대 팀의 현재 전술 (AI 가 경기 중 바꾼 내용이 반영됨) */
public record OpponentTacticResponse(
        Long teamId,
        String teamName,
        String formation,
        String mentality,
        String mentalityLabel,
        String pressing,
        String pressingLabel,
        String lineHeight,
        String lineHeightLabel,
        String attackStyle,
        String attackStyleLabel,
        boolean live,                 // true = 경기 중 실시간 상태, false = 예상(경기 전)
        List<String> recentChanges    // 상대 전술 변화 로그(최근 순)
) {}
