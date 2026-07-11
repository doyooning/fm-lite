'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { matchesApi } from '@/lib/api';
import { Card, ErrorBox, LinkButton, Spinner } from '@/components/ui';
import type { MatchResult } from '@/types/api';

function StatRow({ label, home, away }: { label: string; home: number; away: number }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="w-12 text-left font-semibold tabular-nums">{home}</span>
      <span className="text-xs text-zinc-500">{label}</span>
      <span className="w-12 text-right font-semibold tabular-nums">{away}</span>
    </div>
  );
}

export default function MatchResultPage() {
  const { saveGameId, matchId } = useParams<{ saveGameId: string; matchId: string }>();
  const [result, setResult] = useState<MatchResult | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    matchesApi.result(Number(matchId)).then(setResult).catch((e) => setError(e.message));
  }, [matchId]);

  if (error) return <main className="mx-auto max-w-2xl p-6"><ErrorBox message={error} /></main>;
  if (!result) return <Spinner />;

  const pen = result.penaltyHomeScore !== null;
  const banner =
    result.userWon === null ? null : result.userWon
      ? { text: result.saveGameStatus === 'CHAMPION' ? '🏆 우승을 차지했습니다!' : '승리! 다음 라운드로 진출합니다.', cls: 'border-emerald-500/40 bg-emerald-500/10 text-emerald-300' }
      : { text: '패배... 이번 대회는 여기까지입니다.', cls: 'border-red-500/30 bg-red-500/10 text-red-300' };

  return (
    <main className="mx-auto w-full max-w-2xl p-6">
      {banner && (
        <Card className={`text-center text-lg font-bold ${banner.cls}`}>{banner.text}</Card>
      )}

      <Card className="mt-4">
        <p className="text-center text-xs font-semibold text-emerald-500">{result.roundLabel} · 최종 결과</p>
        <div className="mt-2 flex items-center justify-center gap-4">
          <span className="flex-1 text-right text-lg font-bold">{result.homeTeam.name}</span>
          <span className="rounded-lg bg-zinc-800 px-4 py-1.5 text-3xl font-black tabular-nums">
            {result.homeScore} : {result.awayScore}
          </span>
          <span className="flex-1 text-lg font-bold">{result.awayTeam.name}</span>
        </div>
        {pen && (
          <p className="mt-2 text-center text-sm text-zinc-400">
            승부차기 {result.penaltyHomeScore} : {result.penaltyAwayScore}
          </p>
        )}
      </Card>

      <Card className="mt-4">
        <h2 className="mb-3 text-center text-xs font-semibold text-zinc-500">경기 통계</h2>
        <div className="flex flex-col gap-2">
          <StatRow label="점유율 (%)" home={result.stats.possessionHome} away={result.stats.possessionAway} />
          <StatRow label="슛" home={result.stats.shotsHome} away={result.stats.shotsAway} />
          <StatRow label="유효 슛" home={result.stats.shotsOnTargetHome} away={result.stats.shotsOnTargetAway} />
        </div>
        <p className="mt-4 text-center text-sm">
          <span className="text-xs text-zinc-500">경기 MVP</span>{' '}
          <span className="font-semibold text-amber-300">{result.stats.bestPlayerName}</span>
        </p>
      </Card>

      <div className="mt-6 grid gap-2 sm:grid-cols-2">
        <LinkButton href={`/game/${saveGameId}/competition`}>대진표 확인</LinkButton>
        <LinkButton href={`/game/${saveGameId}`} variant="primary">
          {result.userWon && result.saveGameStatus === 'IN_PROGRESS' ? '다음 경기 준비' : '허브로 돌아가기'}
        </LinkButton>
      </div>
    </main>
  );
}
