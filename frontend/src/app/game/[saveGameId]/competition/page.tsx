'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { competitionsApi, saveGamesApi } from '@/lib/api';
import BracketView from '@/components/BracketView';
import { BackButton, Card, ErrorBox, Spinner } from '@/components/ui';
import type { Bracket, SaveGame } from '@/types/api';

export default function CompetitionPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const [saveGame, setSaveGame] = useState<SaveGame | null>(null);
  const [bracket, setBracket] = useState<Bracket | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    saveGamesApi.get(Number(saveGameId))
      .then((sg) => {
        setSaveGame(sg);
        return competitionsApi.bracket(sg.competitionId);
      })
      .then(setBracket)
      .catch((e) => setError(e.message));
  }, [saveGameId]);

  if (error) return <main className="mx-auto max-w-4xl p-6"><ErrorBox message={error} /></main>;
  if (!saveGame || !bracket) return <Spinner />;

  const championName = bracket.winnerTeamId
    ? bracket.rounds.flatMap((r) => r.matches)
        .flatMap((m) => [m.homeTeam, m.awayTeam])
        .find((t) => t.id === bracket.winnerTeamId)?.name
    : null;

  return (
    <main className="mx-auto w-full max-w-4xl p-6">
      <BackButton fallbackHref={`/game/${saveGameId}`} />
      <div className="flex items-baseline justify-between">
        <h1 className="text-2xl font-bold">{bracket.name}</h1>
        <span className="text-sm text-zinc-400">
          {bracket.currentRound === 'FINISHED' ? '대회 종료' : `${bracket.currentRoundLabel} 진행 중`}
        </span>
      </div>

      {championName && (
        <Card className={`mt-4 text-center ${bracket.winnerTeamId === saveGame.team.id
          ? 'border-amber-500/40 bg-amber-500/10' : 'border-zinc-700'}`}>
          <p className="text-sm text-zinc-400">우승</p>
          <p className="mt-1 text-xl font-bold text-amber-300">🏆 {championName}</p>
        </Card>
      )}

      <div className="mt-6">
        <BracketView bracket={bracket} userTeamId={saveGame.team.id} />
      </div>

      <p className="mt-4 text-xs text-zinc-600">★ 내 팀 경기 · 승자는 초록색으로 표시됩니다.</p>
    </main>
  );
}
