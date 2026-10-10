import type { Formation, Player } from '@/types/api';

export type Pos = 'GK' | 'DF' | 'MF' | 'FW';
export const POSITION_ORDER: Pos[] = ['GK', 'DF', 'MF', 'FW'];
export const POSITION_LABEL: Record<Pos, string> = { GK: '골키퍼', DF: '수비수', MF: '미드필더', FW: '공격수' };

/** 포메이션별 슬롯 역할 인원 (백엔드 Formation.getSlots 와 동일) */
export const FORMATION_SLOTS: Record<Formation, Record<Pos, number>> = {
  '4-3-3': { GK: 1, DF: 4, MF: 3, FW: 3 },
  '4-2-3-1': { GK: 1, DF: 4, MF: 5, FW: 1 },
  '3-5-2': { GK: 1, DF: 3, MF: 5, FW: 2 },
};

export interface SlotSpot {
  role: Pos;
  /** 피치 위 위치(%) — x: 왼쪽→오른쪽, y: 위(공격)→아래(골키퍼) */
  x: number;
  y: number;
}

/**
 * 포메이션별 슬롯 배치. 배열 순서 = 라인업 인덱스 = 백엔드 슬롯 순서(GK → DF → MF → FW).
 * 라인업의 i번째 선수는 i번째 슬롯 역할을 수행하며, 선수의 본래 포지션과는 무관하다.
 */
export const FORMATION_LAYOUT: Record<Formation, SlotSpot[]> = {
  '4-3-3': [
    { role: 'GK', x: 50, y: 90 },
    { role: 'DF', x: 13, y: 70 }, { role: 'DF', x: 38, y: 74 }, { role: 'DF', x: 62, y: 74 }, { role: 'DF', x: 87, y: 70 },
    { role: 'MF', x: 24, y: 49 }, { role: 'MF', x: 50, y: 55 }, { role: 'MF', x: 76, y: 49 },
    { role: 'FW', x: 16, y: 22 }, { role: 'FW', x: 50, y: 15 }, { role: 'FW', x: 84, y: 22 },
  ],
  '4-2-3-1': [
    { role: 'GK', x: 50, y: 90 },
    { role: 'DF', x: 13, y: 70 }, { role: 'DF', x: 38, y: 74 }, { role: 'DF', x: 62, y: 74 }, { role: 'DF', x: 87, y: 70 },
    { role: 'MF', x: 36, y: 55 }, { role: 'MF', x: 64, y: 55 },
    { role: 'MF', x: 16, y: 34 }, { role: 'MF', x: 50, y: 37 }, { role: 'MF', x: 84, y: 34 },
    { role: 'FW', x: 50, y: 14 },
  ],
  '3-5-2': [
    { role: 'GK', x: 50, y: 90 },
    { role: 'DF', x: 22, y: 72 }, { role: 'DF', x: 50, y: 76 }, { role: 'DF', x: 78, y: 72 },
    { role: 'MF', x: 9, y: 46 }, { role: 'MF', x: 30, y: 55 }, { role: 'MF', x: 50, y: 48 }, { role: 'MF', x: 70, y: 55 }, { role: 'MF', x: 91, y: 46 },
    { role: 'FW', x: 35, y: 18 }, { role: 'FW', x: 65, y: 18 },
  ],
};

/** 포메이션 기준 베스트 XI (포지션별 종합치 상위 N). 슬롯 순서(GK→DF→MF→FW)로 반환 — 백엔드 LineupSelector.select 와 동일 */
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

/**
 * 포메이션을 바꿀 때 선발 11명은 유지하되, 가능한 한 본래 포지션에 맞는 슬롯에 재배치한다.
 * (맞는 자리가 없으면 남은 슬롯에 순서대로 채운다)
 */
export function remapLineup(lineup: number[], squad: Player[], formation: Formation): number[] {
  const byId = new Map(squad.map((p) => [p.id, p]));
  const layout = FORMATION_LAYOUT[formation];
  const pool = lineup.filter((id) => byId.has(id));
  const result: (number | undefined)[] = layout.map(() => undefined);
  layout.forEach((slot, i) => {
    const k = pool.findIndex((id) => byId.get(id)!.position === slot.role);
    if (k >= 0) { result[i] = pool[k]; pool.splice(k, 1); }
  });
  return result.map((id) => id ?? pool.shift()).filter((id): id is number => id != null);
}

/** 라인업이 유효한지: 포메이션 슬롯 수만큼, 중복 없이, 모두 우리 팀 선수 (포지션 제약 없음) */
export function isValidLineup(lineup: number[] | undefined, squad: Player[], formation: Formation): boolean {
  const n = FORMATION_LAYOUT[formation].length;
  if (!lineup || lineup.length !== n || new Set(lineup).size !== n) return false;
  const ids = new Set(squad.map((p) => p.id));
  return lineup.every((id) => ids.has(id));
}
