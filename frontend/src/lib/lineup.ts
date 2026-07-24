import type { Formation, Player } from '@/types/api';

export type Pos = 'GK' | 'DF' | 'MF' | 'FW';
export const POSITION_ORDER: Pos[] = ['GK', 'DF', 'MF', 'FW'];
export const POSITION_LABEL: Record<Pos, string> = { GK: '골키퍼', DF: '수비수', MF: '미드필더', FW: '공격수' };

/** 포메이션별 포지션 인원 (백엔드 Formation.getSlots 와 동일) */
export const FORMATION_SLOTS: Record<Formation, Record<Pos, number>> = {
  '4-3-3': { GK: 1, DF: 4, MF: 3, FW: 3 },
  '4-2-3-1': { GK: 1, DF: 4, MF: 5, FW: 1 },
  '3-5-2': { GK: 1, DF: 3, MF: 5, FW: 2 },
};

/**
 * 피치 배치용 라인 구성 (위=공격 → 아래=골키퍼).
 * 같은 포지션이라도 시각적으로 나눠 배치하기 위해 라인 단위로 정의한다.
 * (예: 4-2-3-1 의 MF 5명 = 공격형 3 + 수비형 2)
 */
export const FORMATION_ROWS: Record<Formation, { pos: Pos; count: number }[]> = {
  '4-3-3': [
    { pos: 'FW', count: 3 },
    { pos: 'MF', count: 3 },
    { pos: 'DF', count: 4 },
    { pos: 'GK', count: 1 },
  ],
  '4-2-3-1': [
    { pos: 'FW', count: 1 },
    { pos: 'MF', count: 3 },
    { pos: 'MF', count: 2 },
    { pos: 'DF', count: 4 },
    { pos: 'GK', count: 1 },
  ],
  '3-5-2': [
    { pos: 'FW', count: 2 },
    { pos: 'MF', count: 3 },
    { pos: 'MF', count: 2 },
    { pos: 'DF', count: 3 },
    { pos: 'GK', count: 1 },
  ],
};

/** 포메이션 기준 베스트 XI (포지션별 종합치 상위 N) — 백엔드 LineupSelector.select 와 동일 규칙 */
export function bestEleven(squad: Player[], formation: Formation): number[] {
  const slots = FORMATION_SLOTS[formation];
  const lineup: number[] = [];
  for (const pos of POSITION_ORDER) {
    squad
      .filter((p) => p.position === pos)
      .sort((a, b) => b.overall - a.overall)
      .slice(0, slots[pos])
      .forEach((p) => lineup.push(p.id));
  }
  return lineup;
}

/** 라인업이 포메이션 규칙(포지션별 인원, 11명, 중복없음)에 맞는지 */
export function isValidLineup(lineup: number[] | undefined, squad: Player[], formation: Formation): boolean {
  if (!lineup || lineup.length !== 11 || new Set(lineup).size !== 11) return false;
  const byId = new Map(squad.map((p) => [p.id, p]));
  if (!lineup.every((id) => byId.has(id))) return false;
  const slots = FORMATION_SLOTS[formation];
  for (const pos of POSITION_ORDER) {
    const n = lineup.filter((id) => byId.get(id)!.position === pos).length;
    if (n !== slots[pos]) return false;
  }
  return true;
}
