'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { matchesApi, saveGamesApi } from '@/lib/api';
import { Card, ErrorBox, GradeBadge, LinkButton, Spinner } from '@/components/ui';
import type { OpponentAnalysis } from '@/types/api';

const tacticLabels: Record<string, string> = {
  ATTACKING: '공격적', BALANCED: '균형', DEFENSIVE: '수비적',
  LOW: '낮음', NORMAL: '보통', HIGH: '높음',
  CENTER: '중앙 공격', WIDE: '측면 공격', COUNTER: '역습', POSSESSION: '점유',
};

function CompareBar({ label, mine, theirs }: { label: string; mine: number; theirs: number }) {
  const total = mine + theirs;
  const minePct = (mine / total) * 100;
  return (
    <div>
      <div className="flex justify-between text-xs text-zinc-400">
        <span className="tabular-nums font-semibold text-emerald-400">{mine}</span>
        <span>{label}</span>
        <span className="tabular-nums font-semibold text-red-400">{theirs}</span>
      </div>
      <div className="mt-1 flex h-2 overflow-hidden rounded-full bg-zinc-800">
        <div className="bg-emerald-500" style={{ width: `${minePct}%` }} />
        <div className="bg-red-500/70" style={{ width: `${100 - minePct}%` }} />
      </div>
    </div>
  );
}

export default function OpponentPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const id = Number(saveGameId);
  const [analysis, setAnalysis] = useState<OpponentAnalysis | null>(null);
  const [noMatch, setNoMatch] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    saveGamesApi.nextMatch(id)
      .then((next) => {
        if (!next.hasNext || !next.match) { setNoMatch(true); return null; }
        return matchesApi.opponentAnalysis(next.match.matchId);
      })
      .then((a) => a && setAnalysis(a))
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <main className="mx-auto max-w-3xl p-6"><ErrorBox message={error} /></main>;
  if (noMatch) {
    return (
      <main className="mx-auto max-w-3xl p-6">
        <Card className="text-center text-sm text-zinc-400">다음 경기가 없어 분석할 상대가 없습니다.</Card>
      </main>
    );
  }
  if (!analysis) return <Spinner text="상대 팀을 분석하는 중..." />;

  const t = analysis.expectedTactic;

  return (
    <main className="mx-auto w-full max-w-3xl p-6">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-bold">{analysis.team.name}</h1>
        <GradeBadge grade={analysis.team.grade} label={analysis.team.gradeLabel} />
      </div>
      <p className="mt-1 text-sm text-zinc-400">코치진이 정리한 다음 상대 분석 리포트입니다.</p>

      <Card className="mt-5">
        <h2 className="text-sm font-semibold text-zinc-300">전력 비교 <span className="text-xs font-normal text-zinc-500">(우리 팀 vs 상대)</span></h2>
        <div className="mt-3 flex flex-col gap-3">
          <CompareBar label="공격" mine={analysis.myPowerByArea.attack} theirs={analysis.powerByArea.attack} />
          <CompareBar label="중원" mine={analysis.myPowerByArea.midfield} theirs={analysis.powerByArea.midfield} />
          <CompareBar label="수비" mine={analysis.myPowerByArea.defense} theirs={analysis.powerByArea.defense} />
          <CompareBar label="골키퍼" mine={analysis.myPowerByArea.goalkeeping} theirs={analysis.powerByArea.goalkeeping} />
        </div>
      </Card>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <Card>
          <h2 className="text-sm font-semibold text-emerald-400">강점</h2>
          <ul className="mt-2 flex flex-col gap-1.5 text-sm text-zinc-300">
            {analysis.strengths.map((s, i) => <li key={i}>· {s}</li>)}
          </ul>
        </Card>
        <Card>
          <h2 className="text-sm font-semibold text-red-400">약점</h2>
          <ul className="mt-2 flex flex-col gap-1.5 text-sm text-zinc-300">
            {analysis.weaknesses.map((s, i) => <li key={i}>· {s}</li>)}
          </ul>
        </Card>
      </div>

      <Card className="mt-4">
        <h2 className="text-sm font-semibold text-zinc-300">경계 대상</h2>
        <ul className="mt-2 flex flex-col gap-1 text-sm">
          {analysis.keyPlayers.map((p) => (
            <li key={p.id} className="flex items-center justify-between">
              <span>{p.name} <span className="text-xs text-zinc-500">{p.position} · {p.reason}</span></span>
              <span className="font-semibold tabular-nums text-zinc-300">{p.overall}</span>
            </li>
          ))}
        </ul>
      </Card>

      <Card className="mt-4">
        <h2 className="text-sm font-semibold text-zinc-300">예상 전술</h2>
        <div className="mt-2 flex flex-wrap gap-2 text-sm">
          <span className="rounded bg-zinc-800 px-2 py-1">{t.formation}</span>
          <span className="rounded bg-zinc-800 px-2 py-1">{tacticLabels[t.mentality]}</span>
          <span className="rounded bg-zinc-800 px-2 py-1">압박 {tacticLabels[t.pressing]}</span>
          <span className="rounded bg-zinc-800 px-2 py-1">라인 {tacticLabels[t.lineHeight]}</span>
          <span className="rounded bg-zinc-800 px-2 py-1">{tacticLabels[t.attackStyle]}</span>
        </div>
      </Card>

      <div className="mt-6 flex justify-end">
        <LinkButton href={`/game/${id}/tactics`} variant="primary">전술 설정하러 가기</LinkButton>
      </div>
    </main>
  );
}
