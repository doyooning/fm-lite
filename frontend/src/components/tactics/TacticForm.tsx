'use client';

import type { Formation, Tactic, Player } from '@/types/api';
import { bestEleven, remapLineup } from '@/lib/lineup';
import LineupEditor from './LineupEditor';

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

  const changeFormation = (formation: Formation) => {
    if (formation === value.formation) return;
    // 선발 11명은 유지하고, 가능한 한 본래 포지션에 맞는 슬롯으로 재배치
    onChange({ ...value, formation, lineup: remapLineup(lineup, squad, formation) });
  };

  return (
    <div className="flex flex-col gap-5">
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

      {/* 선발 라인업 — 피치 + 선수단 드래그 앤 드롭 */}
      <div>
        <div className="mb-2 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-300">선발 라인업</h3>
          <button
            onClick={() => onChange({ ...value, lineup: bestEleven(squad, value.formation) })}
            className="rounded-md border border-zinc-700 bg-zinc-900 px-2 py-1 text-xs text-zinc-300 hover:border-zinc-500"
          >
            베스트 XI 자동
          </button>
        </div>
        <LineupEditor
          formation={value.formation}
          lineup={lineup}
          squad={squad}
          stacked={compact}
          onChange={(l) => onChange({ ...value, lineup: l })}
        />
      </div>

      {/* 전술 옵션 */}
      <div className="flex flex-col gap-3">
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
      </div>
    </div>
  );
}
