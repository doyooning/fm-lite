'use client';

import { useEffect, useState } from 'react';
import { saveGamesApi } from '@/lib/api';
import { getSaveGameId } from '@/lib/storage';
import { Card, GradeBadge, LinkButton } from '@/components/ui';
import type { SaveGame } from '@/types/api';

const statusLabel: Record<SaveGame['status'], string> = {
  IN_PROGRESS: '진행 중',
  CHAMPION: '🏆 우승',
  ELIMINATED: '탈락',
};

export default function HomePage() {
  const [saveGame, setSaveGame] = useState<SaveGame | null>(null);

  useEffect(() => {
    const id = getSaveGameId();
    if (id) saveGamesApi.get(id).then(setSaveGame).catch(() => setSaveGame(null));
  }, []);

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-lg flex-col items-center justify-center gap-8 p-6">
      <div className="text-center">
        <p className="text-sm font-semibold tracking-widest text-emerald-500">FOOTBALL MANAGER LITE</p>
        <h1 className="mt-2 text-5xl font-black">FM Lite</h1>
        <p className="mt-3 text-zinc-400">
          팀을 선택하고, 전술을 짜고, 8팀 토너먼트 우승에 도전하세요.
        </p>
      </div>

      <div className="flex w-full flex-col gap-3">
        <LinkButton href="/new-game" variant="primary" className="py-3 text-base">
          새 게임 시작
        </LinkButton>

        {saveGame && (
          <Card>
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold">{saveGame.team.name}</span>
                  <GradeBadge grade={saveGame.teamGrade} label={statusLabel[saveGame.status]} />
                </div>
                <p className="mt-1 text-sm text-zinc-400">
                  FM 챔피언스 컵 · {saveGame.currentRoundLabel}
                </p>
              </div>
              <LinkButton href={`/game/${saveGame.id}`}>이어하기</LinkButton>
            </div>
          </Card>
        )}
      </div>

      <p className="text-xs text-zinc-600">가상 팀 · 가상 선수 · 규칙 기반 시뮬레이션</p>
    </main>
  );
}
