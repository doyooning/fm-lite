'use client';

import { useMemo } from 'react';
import type { Formation, Player, Tactic } from '@/types/api';
import {
  bestEleven, FORMATION_ROWS, POSITION_LABEL, POSITION_ORDER, type Pos,
} from '@/lib/lineup';

const FORMATIONS: Formation[] = ['4-3-3', '4-2-3-1', '3-5-2'];
const formationDesc: Record<Formation, string> = {
  '4-3-3': '공격수 3명 — 화력 중심',
  '4-2-3-1': '미드필더 5명 — 중원 장악',
  '3-5-2': '변칙 3백 — 중원 수적 우위',
};

type OptionKey = 'mentality' | 'pressing' | 'lineHeight' | 'attackStyle';
const groups: { key: OptionKey; title: string; hint: string; options: { value: string; label: string }[] }[] = [
  { key: 'mentality', title: '성향', hint: '공격적일수록 찬스가 늘지만 실점 위험도 커집니다.',
    options: [{ value: 'ATTACKING', label: '공격적' }, { value: 'BALANCED', label: '균형' }, { value: 'DEFENSIVE', label: '수비적' }] },
  { key: 'pressing', title: '압박 강도', hint: '높은 압박은 상대를 흔들지만 체력 소모가 큽니다.',
    options: [{ value: 'LOW', label: '낮음' }, { value: 'NORMAL', label: '보통' }, { value: 'HIGH', label: '높음' }] },
  { key: 'lineHeight', title: '수비 라인', hint: '높은 라인은 상대가 뒷공간 침투로 노릴 수 있습니다.',
    options: [{ value: 'LOW', label: '낮음' }, { value: 'NORMAL', label: '보통' }, { value: 'HIGH', label: '높음' }] },
  { key: 'attackStyle', title: '공격 방식', hint: '상대 전술과의 상성이 경기에 영향을 줍니다.',
    options: [{ value: 'CENTER', label: '중앙' }, { value: 'WIDE', label: '측면' }, { value: 'COUNTER', label: '역습' }, { value: 'POSSESSION', label: '점유' }] },
];

export default function TacticForm({
  value, squad, onChange, compact = false,
}: {
  value: Tactic;
  squad: Player[];
  onChange: (t: Tactic) => void;
  compact?: boolean;
}) {
  const lineup = value.lineup ?? bestEleven(squad, value.formation);
  const byId = useMemo(() => new Map(squad.map((p) => [p.id, p])), [squad]);

  // 피치 라인 구성과, 같은 포지션이 여러 라인에 나뉠 때의 시작 인덱스
  const rows = FORMATION_ROWS[value.formation];
  const rowOffsets = useMemo(() => {
    const seen: Partial<Record<Pos, number>> = {};
    return rows.map((r) => {
      const start = seen[r.pos] ?? 0;
      seen[r.pos] = start + r.count;
      return start;
    });
  }, [rows]);

  const changeFormation = (formation: Formation) => {
    // 포메이션이 바뀌면 포지션 인원이 달라지므로 베스트 XI 로 재설정
    onChange({ ...value, formation, lineup: bestEleven(squad, formation) });
  };

  const selectedByPos = (pos: Pos) => lineup.filter((id) => byId.get(id)?.position === pos);

  const swapSlot = (pos: Pos, slotIndex: number, newId: number) => {
    const current = selectedByPos(pos);
    const prevId = current[slotIndex];
    if (prevId === newId) return;
    // 같은 포지션 다른 슬롯에 이미 있으면 자리 교환, 없으면 교체
    const next = [...current];
    const dupIndex = next.indexOf(newId);
    if (dupIndex >= 0) next[dupIndex] = prevId;
    next[slotIndex] = newId;
    // 전체 라인업 재구성 (GK→DF→MF→FW 순서 유지)
    const rebuilt: number[] = [];
    for (const p of POSITION_ORDER) rebuilt.push(...(p === pos ? next : selectedByPos(p)));
    onChange({ ...value, lineup: rebuilt });
  };

  return (
    <div className="flex flex-col gap-3">
      {/* 포메이션 */}
      <div>
        <h3 className="mb-2 text-sm font-semibold text-zinc-300">포메이션</h3>
        <div className="grid gap-2 sm:grid-cols-3">
          {FORMATIONS.map((f) => (
            <button
              key={f}
              onClick={() => changeFormation(f)}
              className={`rounded-lg border p-3 text-left transition ${
                value.formation === f ? 'border-emerald-500 bg-emerald-500/10' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
              }`}
            >
              <p className="font-bold">{f}</p>
              {!compact && <p className="mt-1 text-xs text-zinc-400">{formationDesc[f]}</p>}
            </button>
          ))}
        </div>
      </div>

      {/* 옵션 그룹 */}
      {groups.map((g) => (
        <div key={g.key}>
          <div className="flex items-baseline justify-between gap-3">
            <h3 className="text-sm font-semibold text-zinc-300">{g.title}</h3>
            {!compact && <p className="text-xs text-zinc-500">{g.hint}</p>}
          </div>
          <div className="mt-1.5 flex gap-2">
            {g.options.map((o) => (
              <button
                key={o.value}
                onClick={() => onChange({ ...value, [g.key]: o.value })}
                className={`flex-1 rounded-lg border px-3 py-2 text-sm transition ${
                  value[g.key] === o.value ? 'border-emerald-500 bg-emerald-500/10 font-semibold' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                }`}
              >
                {o.label}
              </button>
            ))}
          </div>
        </div>
      ))}

      {/* 선발 라인업 */}
      <div>
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-300">선발 라인업 <span className="text-xs font-normal text-zinc-500">(포지션별 교체 가능)</span></h3>
          <button
            onClick={() => onChange({ ...value, lineup: bestEleven(squad, value.formation) })}
            className="rounded-md border border-zinc-700 bg-zinc-900 px-2 py-1 text-xs text-zinc-300 hover:border-zinc-500"
          >
            베스트 XI 자동
          </button>
        </div>
        {/* 포메이션대로 배치된 피치 뷰 (위=공격, 아래=골키퍼). 각 슬롯에서 선수 교체 */}
        <div className="mt-2 rounded-xl border border-emerald-900/60 bg-gradient-to-b from-emerald-950 to-emerald-900/40 p-3">
          <div className="flex flex-col gap-2.5">
            {rows.map((row, rowIdx) => {
              const selected = selectedByPos(row.pos);
              const options = squad
                .filter((p) => p.position === row.pos)
                .sort((a, b) => b.overall - a.overall);
              return (
                <div key={rowIdx} className="flex justify-center gap-2">
                  {Array.from({ length: row.count }).map((_, i) => {
                    const slotIndex = rowOffsets[rowIdx] + i;
                    const selId = selected[slotIndex];
                    const player = selId != null ? byId.get(selId) : undefined;
                    return (
                      <div key={i} className="w-full max-w-[124px] min-w-0">
                        <select
                          value={selId ?? ''}
                          onChange={(e) => swapSlot(row.pos, slotIndex, Number(e.target.value))}
                          title={player ? `${player.name} (${player.overall})` : ''}
                          className="w-full truncate rounded-lg border border-emerald-600/50 bg-zinc-900/90 px-1 py-1.5 text-center text-xs font-medium text-zinc-100 focus:border-emerald-400 focus:outline-none"
                        >
                          {options.map((p) => {
                            const usedElsewhere = selected.includes(p.id) && p.id !== selId;
                            return (
                              <option key={p.id} value={p.id} disabled={usedElsewhere}>
                                {p.name} ({p.overall}){usedElsewhere ? ' · 선발중' : ''}
                              </option>
                            );
                          })}
                        </select>
                        <p className="mt-0.5 text-center text-[10px] text-emerald-300/70">
                          {POSITION_LABEL[row.pos].slice(0, 2)}
                          {player ? ` · ${player.overall}` : ''}
                        </p>
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
