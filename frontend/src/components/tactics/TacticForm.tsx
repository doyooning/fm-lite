'use client';

import { useMemo } from 'react';
import type { Formation, Player, Tactic } from '@/types/api';
import {
  bestEleven, FORMATION_SLOTS, POSITION_LABEL, POSITION_ORDER, type Pos,
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
        <div className="mt-2 flex flex-col gap-2">
          {POSITION_ORDER.map((pos) => {
            const count = FORMATION_SLOTS[value.formation][pos];
            const selected = selectedByPos(pos);
            const options = squad.filter((p) => p.position === pos).sort((a, b) => b.overall - a.overall);
            return (
              <div key={pos} className="flex items-start gap-2">
                <span className="mt-2 w-8 shrink-0 text-xs font-semibold text-zinc-500">{POSITION_LABEL[pos].slice(0, 2)}</span>
                <div className="grid flex-1 gap-1.5 sm:grid-cols-2">
                  {Array.from({ length: count }).map((_, i) => {
                    const selId = selected[i];
                    return (
                      <select
                        key={i}
                        value={selId ?? ''}
                        onChange={(e) => swapSlot(pos, i, Number(e.target.value))}
                        className="rounded-md border border-zinc-800 bg-zinc-900 px-2 py-1.5 text-sm text-zinc-100 focus:border-emerald-500 focus:outline-none"
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
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
