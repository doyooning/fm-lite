'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { matchesApi, saveGamesApi } from '@/lib/api';
import { Button, Card, ErrorBox, Spinner } from '@/components/ui';
import type { Tactic } from '@/types/api';

const FORMATIONS: Tactic['formation'][] = ['4-3-3', '4-2-3-1', '3-5-2'];
const formationDesc: Record<string, string> = {
  '4-3-3': '공격수 3명 — 화력 중심',
  '4-2-3-1': '미드필더 5명 — 중원 장악',
  '3-5-2': '변칙 3백 — 중원 수적 우위',
};

type OptionGroup<K extends keyof Tactic> = {
  key: K;
  title: string;
  hint: string;
  options: { value: Tactic[K]; label: string }[];
};

const groups: OptionGroup<'mentality' | 'pressing' | 'lineHeight' | 'attackStyle'>[] = [
  {
    key: 'mentality', title: '성향', hint: '공격적일수록 찬스가 늘지만 실점 위험도 커집니다.',
    options: [
      { value: 'ATTACKING', label: '공격적' },
      { value: 'BALANCED', label: '균형' },
      { value: 'DEFENSIVE', label: '수비적' },
    ],
  },
  {
    key: 'pressing', title: '압박 강도', hint: '높은 압박은 상대를 흔들지만 체력 소모가 큽니다.',
    options: [
      { value: 'LOW', label: '낮음' },
      { value: 'NORMAL', label: '보통' },
      { value: 'HIGH', label: '높음' },
    ],
  },
  {
    key: 'lineHeight', title: '수비 라인', hint: '높은 라인은 상대가 뒷공간 침투로 노릴 수 있습니다.',
    options: [
      { value: 'LOW', label: '낮음' },
      { value: 'NORMAL', label: '보통' },
      { value: 'HIGH', label: '높음' },
    ],
  },
  {
    key: 'attackStyle', title: '공격 방식', hint: '상대 전술과의 상성이 경기에 영향을 줍니다.',
    options: [
      { value: 'CENTER', label: '중앙' },
      { value: 'WIDE', label: '측면' },
      { value: 'COUNTER', label: '역습' },
      { value: 'POSSESSION', label: '점유' },
    ],
  },
];

export default function TacticsPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const id = Number(saveGameId);
  const router = useRouter();
  const [matchId, setMatchId] = useState<number | null>(null);
  const [tactic, setTactic] = useState<Tactic | null>(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    saveGamesApi.nextMatch(id)
      .then((next) => {
        if (!next.hasNext || !next.match) throw new Error('전술을 설정할 다음 경기가 없습니다.');
        setMatchId(next.match.matchId);
        return matchesApi.getMyTactic(next.match.matchId);
      })
      .then((res) => setTactic(res.tactic))
      .catch((e) => setError(e.message));
  }, [id]);

  const save = async (thenStart: boolean) => {
    if (!matchId || !tactic) return;
    setSaving(true);
    try {
      await matchesApi.saveMyTactic(matchId, tactic);
      router.push(thenStart ? `/game/${id}/match/${matchId}` : `/game/${id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장 실패');
      setSaving(false);
    }
  };

  if (error) return <main className="mx-auto max-w-3xl p-6"><ErrorBox message={error} /></main>;
  if (!tactic) return <Spinner />;

  return (
    <main className="mx-auto w-full max-w-3xl p-6">
      <h1 className="text-2xl font-bold">경기 전 전술 설정</h1>
      <p className="mt-1 text-sm text-zinc-400">킥오프 전 마지막 준비입니다. 경기 시작 후에는 선택지 이벤트로만 개입할 수 있습니다.</p>

      <Card className="mt-5">
        <h2 className="text-sm font-semibold text-zinc-300">포메이션</h2>
        <div className="mt-2 grid gap-2 sm:grid-cols-3">
          {FORMATIONS.map((f) => (
            <button
              key={f}
              onClick={() => setTactic({ ...tactic, formation: f })}
              className={`rounded-lg border p-3 text-left transition ${
                tactic.formation === f
                  ? 'border-emerald-500 bg-emerald-500/10'
                  : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
              }`}
            >
              <p className="font-bold">{f}</p>
              <p className="mt-1 text-xs text-zinc-400">{formationDesc[f]}</p>
            </button>
          ))}
        </div>
      </Card>

      {groups.map((g) => (
        <Card key={g.key} className="mt-4">
          <div className="flex items-baseline justify-between gap-3">
            <h2 className="text-sm font-semibold text-zinc-300">{g.title}</h2>
            <p className="text-xs text-zinc-500">{g.hint}</p>
          </div>
          <div className="mt-2 flex gap-2">
            {g.options.map((o) => (
              <button
                key={o.value}
                onClick={() => setTactic({ ...tactic, [g.key]: o.value })}
                className={`flex-1 rounded-lg border px-3 py-2 text-sm transition ${
                  tactic[g.key] === o.value
                    ? 'border-emerald-500 bg-emerald-500/10 font-semibold'
                    : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                }`}
              >
                {o.label}
              </button>
            ))}
          </div>
        </Card>
      ))}

      <div className="mt-6 flex justify-end gap-2">
        <Button variant="secondary" onClick={() => save(false)} disabled={saving}>저장</Button>
        <Button onClick={() => save(true)} disabled={saving}>
          {saving ? '저장 중...' : '저장 후 경기 시작'}
        </Button>
      </div>
    </main>
  );
}
