package com.fmlite.player;

import lombok.Getter;

@Getter
public enum PlayerTrait {
    BIG_GAME_PLAYER("빅게임에 강함", "4강·결승 등 큰 경기에서 능력치가 상승합니다.", true),
    LOW_STAMINA("체력 약함", "후반이 되면 체력 저하가 남들보다 큽니다.", false),
    WEAK_UNDER_PRESSURE("압박에 약함", "상대가 강하게 압박하면 패스 능력이 떨어집니다.", false),
    LONG_SHOT("중거리 슛 선호", "중거리에서 과감한 슛을 자주 시도합니다.", true),
    POOR_CONCENTRATION("수비 집중력 낮음", "경기 막판 수비 집중력이 흐트러집니다.", false),
    RUN_IN_BEHIND("침투 선호", "상대 수비 라인이 높으면 뒷공간을 노립니다.", true),
    AERIAL_THREAT("공중볼 강함", "측면 크로스 상황에서 결정력이 올라갑니다.", true),
    PASS_MASTER("패스 마스터", "팀의 중원 장악력을 끌어올립니다.", true),
    PK_SAVER("PK 선방 강함", "승부차기에서 선방 확률이 크게 올라갑니다.", true);

    private final String label;
    private final String description;
    private final boolean positive;   // true=장점(파랑), false=약점(빨강)

    PlayerTrait(String label, String description, boolean positive) {
        this.label = label;
        this.description = description;
        this.positive = positive;
    }
}
