'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { saveGamesApi } from '@/lib/api';
import { Card, ErrorBox, LinkButton, Spinner } from '@/components/ui';
import type { NextMatchResponse, SaveGame } from '@/types/api';

export default function GameHubPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const id = Number(saveGameId);
  const [saveGame, setSaveGame] = useState<SaveGame | null>(null);
  const [next, setNext] = useState<NextMatchResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([saveGamesApi.get(id), saveGamesApi.nextMatch(id)])
      .then(([sg, nm]) => { setSaveGame(sg); setNext(nm); })
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <main className="mx-auto max-w-3xl p-6"><ErrorBox message={error} /></main>;
  if (!saveGame || !next) return <Spinner />;

  const m = next.match;

  return (
    <main className="mx-auto w-full max-w-3xl p-6">
      {saveGame.status === 'CHAMPION' && (
        <Card className="mb-6 border-amber-500/40 bg-amber-500/10 text-center">
          <p className="text-3xl">🏆</p>
          <h2 className="mt-1 text-xl font-bold text-amber-300">FM 챔피언스 컵 우승!</h2>
          <p className="mt-1 text-sm text-zinc-300">{saveGame.team.name}의 감독으로 정상에 올랐습니다.</p>
        </Card>
      )}
      {saveGame.status === 'ELIMINATED' && (
        <Card className="mb-6 border-red-500/30 bg-red-500/5 text-center">
          <h2 className="text-lg font-bold text-red-300">아쉽게 탈락했습니다</h2>
          <p className="mt-1 text-sm text-zinc-400">대진표에서 대회 결과를 확인하거나 새 게임에 도전하세요.</p>
        </Card>
      )}

      {m ? (
        <Card>
          <p className="text-xs font-semibold text-emerald-500">{m.roundLabel} · 다음 경기</p>
          <div className="mt-3 flex items-center justify-center gap-4 text-center">
            <div className="flex-1">
              <p className="text-lg font-bold">{m.homeTeam.name}</p>
              {m.isUserHome && <p className="text-xs text-amber-400">우리 팀</p>}
            </div>
            <span className="text-2xl font-black text-zinc-600">VS</span>
            <div className="flex-1">
              <p className="text-lg font-bold">{m.awayTeam.name}</p>
              {!m.isUserHome && <p className="text-xs text-amber-400">우리 팀</p>}
            </div>
          </div>

          <div className="mt-5 grid gap-2 sm:grid-cols-3">
            <LinkButton href={`/game/${id}/opponent`}>상대 분석</LinkButton>
            <LinkButton href={`/game/${id}/tactics`}>
              전술 설정 {m.tacticSubmitted ? '✓' : ''}
            </LinkButton>
            {m.status === 'SCHEDULED' && !m.tacticSubmitted ? (
              <span className="rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-center text-sm text-zinc-500">
                전술 설정 후 경기 시작
              </span>
            ) : (
              <LinkButton href={`/game/${id}/match/${m.matchId}`} variant="primary">
                {m.status === 'SCHEDULED' ? '경기 시작' : '경기 이어보기'}
              </LinkButton>
            )}
          </div>
        </Card>
      ) : (
        <Card className="text-center text-sm text-zinc-400">
          예정된 경기가 없습니다. 대회가 종료되었습니다.
        </Card>
      )}

      <div className="mt-6 grid gap-3 sm:grid-cols-2">
        <Card>
          <h3 className="text-sm font-semibold text-zinc-300">내 선수단</h3>
          <p className="mt-1 text-xs text-zinc-500">18명 스쿼드의 능력치와 특성을 확인하세요.</p>
          <LinkButton href={`/game/${id}/squad`} className="mt-3 w-full">선수단 보기</LinkButton>
        </Card>
        <Card>
          <h3 className="text-sm font-semibold text-zinc-300">대회 현황</h3>
          <p className="mt-1 text-xs text-zinc-500">8강부터 결승까지 대진표를 확인하세요.</p>
          <LinkButton href={`/game/${id}/competition`} className="mt-3 w-full">대진표 보기</LinkButton>
        </Card>
      </div>
    </main>
  );
}
