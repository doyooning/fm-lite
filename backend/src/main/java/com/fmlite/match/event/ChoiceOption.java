package com.fmlite.match.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** match_events.choice_options(jsonb)에 저장되는 선택지 1건 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceOption {
    private String id;
    private String label;
    private String description;
}
