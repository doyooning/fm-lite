package com.fmlite.match.simulation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/** 이벤트 타입별 중계 텍스트 템플릿 (확장 시 이 계층만 LLM 생성으로 교체) */
@Component
public class MatchEventGenerator {

    // 이름 뒤 조사(가/이) 문제를 피하도록 템플릿은 '{player}의 / {player},' 형태로만 작성한다
    private static final List<String> GOAL_TEMPLATES = List.of(
            "GOAL! {player}의 침착한 마무리! {team}이(가) 골망을 흔듭니다!",
            "골입니다! {player}! 수비 사이를 파고들어 해결합니다!",
            "{player}의 강력한 슛이 그대로 꽂힙니다! {team}의 득점!",
            "환상적인 골! {player}, 각도 없는 곳에서 만들어냅니다!",
            "{team}의 공격이 결실을 맺습니다. 마무리는 {player}!"
    );

    private static final List<String> SAVE_TEMPLATES = List.of(
            "{player}의 슛! 하지만 {gk}의 슈퍼세이브에 막힙니다!",
            "결정적인 찬스! {player}의 슛을 {gk}의 선방이 저지합니다!",
            "{player}의 강한 슛, {gk}의 선방에 막힙니다.",
            "골문을 두드리는 {player}! {gk}, 끝까지 집중력을 유지합니다."
    );

    private static final List<String> MISS_TEMPLATES = List.of(
            "{player}의 슛! 아깝게 골대를 빗나갑니다.",
            "좋은 위치에서 {player}의 시도, 크로스바를 넘깁니다.",
            "{team}의 빠른 전개! 그러나 {player}의 마무리가 아쉽습니다.",
            "{player}, 절호의 기회를 놓칩니다!"
    );

    public String kickOff(String homeName, String awayName) {
        return "경기 시작! " + homeName + " vs " + awayName + ", 휘슬이 울립니다.";
    }

    public String halfTime(int homeScore, int awayScore) {
        return "전반 종료. 스코어 " + homeScore + " : " + awayScore + ".";
    }

    public String fullTime(int homeScore, int awayScore) {
        return "경기 종료! 최종 스코어 " + homeScore + " : " + awayScore + ".";
    }

    public String goal(String team, String player, Random rng) {
        return fill(pick(GOAL_TEMPLATES, rng), team, player, null);
    }

    public String save(String team, String player, String gk, Random rng) {
        return fill(pick(SAVE_TEMPLATES, rng), team, player, gk);
    }

    public String miss(String team, String player, Random rng) {
        return fill(pick(MISS_TEMPLATES, rng), team, player, null);
    }

    public String coachDecision(String managerName, String label) {
        return managerName + " 감독의 지시: [" + label + "] 선수들에게 새로운 지침이 전달됩니다.";
    }

    public String shootoutStart() {
        return "정규시간 무승부! 승부는 페널티킥으로 갈립니다.";
    }

    public String shootoutResult(String winnerName, int homePens, int awayPens) {
        return "승부차기 " + homePens + " : " + awayPens + " — " + winnerName + "이(가) 승리합니다!";
    }

    private String pick(List<String> templates, Random rng) {
        return templates.get(rng.nextInt(templates.size()));
    }

    private String fill(String template, String team, String player, String gk) {
        String s = template.replace("{team}", team).replace("{player}", player);
        return gk == null ? s : s.replace("{gk}", gk);
    }
}
