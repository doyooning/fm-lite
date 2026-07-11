package com.fmlite.match.simulation;

import com.fmlite.match.event.ChoiceOption;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 경기 중 선택지 생성 + 선택 효과 정의 */
@Service
public class MatchChoiceService {

    /** 선택지 효과: 이후 N틱 동안 찬스 확률/질 배수, 즉시 체력 비용 */
    public record EffectSpec(double myChanceMult, double oppChanceMult,
                             double myQualityMult, double oppQualityMult,
                             int durationTicks, double staminaCost) {}

    public record ChoicePrompt(String question, List<ChoiceOption> options) {}

    private static final int REST_OF_MATCH = 99;

    private static final Map<String, EffectSpec> SPECS = Map.of(
            "ALL_OUT_ATTACK", new EffectSpec(1.30, 1.25, 1.0, 1.0, REST_OF_MATCH, 0.03),
            "PARK_BUS",       new EffectSpec(0.70, 1.0, 1.0, 0.85, REST_OF_MATCH, 0.0),
            "COUNTER_FOCUS",  new EffectSpec(0.90, 1.0, 1.25, 1.0, REST_OF_MATCH, 0.0),
            "PUSH_FORWARD",   new EffectSpec(1.20, 1.10, 1.0, 1.0, 4, 0.01),
            "SURGE",          new EffectSpec(1.40, 1.0, 1.0, 1.0, 2, 0.05),
            "STAY_CALM",      new EffectSpec(1.05, 0.95, 1.0, 1.0, 4, 0.0),
            "KEEP_SHAPE",     new EffectSpec(1.0, 0.95, 1.0, 1.0, 4, 0.0)
    );

    public EffectSpec spec(String choiceId) {
        return SPECS.get(choiceId);
    }

    public boolean isValidChoice(String choiceId) {
        return SPECS.containsKey(choiceId);
    }

    public String labelOf(String choiceId) {
        return switch (choiceId) {
            case "ALL_OUT_ATTACK" -> "총공격";
            case "PARK_BUS" -> "수비 강화";
            case "COUNTER_FOCUS" -> "역습 노림";
            case "PUSH_FORWARD" -> "라인 전진";
            case "SURGE" -> "몰아치기";
            case "STAY_CALM" -> "침착하게";
            case "KEEP_SHAPE" -> "대형 유지";
            default -> choiceId;
        };
    }

    /** 스코어/시간/모멘텀 상황에 맞는 선택지 프롬프트 생성 */
    public ChoicePrompt buildPrompt(int myScore, int oppScore, int momentum, int minute) {
        if (myScore < oppScore) {
            return new ChoicePrompt(
                    minute + "분, 우리 팀이 지고 있습니다. 어떻게 대응할까요?",
                    List.of(
                            option("ALL_OUT_ATTACK", "찬스가 크게 늘지만 상대 역습도 위험해집니다."),
                            option("COUNTER_FOCUS", "찬스는 줄지만 한 방의 질이 올라갑니다."),
                            option("KEEP_SHAPE", "현재 흐름을 유지하며 안정을 찾습니다.")
                    ));
        }
        if (myScore > oppScore) {
            return new ChoicePrompt(
                    minute + "분, 리드를 지키고 있습니다. 남은 시간 어떻게 운영할까요?",
                    List.of(
                            option("PARK_BUS", "실점 위험은 줄지만 추가골 기회도 줄어듭니다."),
                            option("COUNTER_FOCUS", "내려앉되 역습 한 방을 노립니다."),
                            option("KEEP_SHAPE", "지금처럼 균형을 유지합니다.")
                    ));
        }
        if (momentum >= 1) {
            return new ChoicePrompt(
                    minute + "분, 팽팽한 승부에서 우리 팀 분위기가 올라오고 있습니다!",
                    List.of(
                            option("SURGE", "짧은 시간 화력을 집중하지만 체력 소모가 큽니다."),
                            option("PUSH_FORWARD", "라인을 올려 꾸준히 압박합니다."),
                            option("KEEP_SHAPE", "무리하지 않고 기회를 기다립니다.")
                    ));
        }
        return new ChoicePrompt(
                minute + "분, 팽팽한 균형이 이어지고 있습니다. 변화가 필요할까요?",
                List.of(
                        option("PUSH_FORWARD", "찬스가 늘지만 뒷공간 위험도 커집니다."),
                        option("STAY_CALM", "안정적으로 볼을 소유하며 틈을 노립니다."),
                        option("KEEP_SHAPE", "현 전술을 신뢰하고 유지합니다.")
                ));
    }

    private ChoiceOption option(String id, String description) {
        return new ChoiceOption(id, labelOf(id), description);
    }
}
