'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi, saveGamesApi } from '@/lib/api';
import { isLoggedIn, logout, setSaveGameId } from '@/lib/auth';
import { Button, Card, GradeBadge, LinkButton, Spinner } from '@/components/ui';
import type { SaveGame, User } from '@/types/api';

const MAX_GAMES = 3;
const statusLabel: Record<SaveGame['status'], string> = {
  IN_PROGRESS: '진행 중',
  CHAMPION: '🏆 우승',
  ELIMINATED: '탈락',
};

export default function HomePage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [games, setGames] = useState<SaveGame[]>([]);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const loadGames = () => saveGamesApi.listMine().then(setGames).catch(() => setGames([]));

  useEffect(() => {
    if (!isLoggedIn()) {
      setReady(true);
      return;
    }
    setLoggedIn(true);
    authApi.me()
      .then((me) => { setUser(me); return loadGames(); })
      .catch(() => { /* 401 시 client 가 로그인으로 보냄 */ })
      .finally(() => setReady(true));
  }, []);

  const startNewGame = () => {
    if (!loggedIn) { router.push('/login'); return; }
    router.push('/new-game');
  };

  const remove = async (id: number) => {
    if (deletingId) return;
    if (!confirm('이 게임을 삭제할까요? 되돌릴 수 없습니다.')) return;
    setDeletingId(id);
    try {
      await saveGamesApi.remove(id);
      setGames((g) => g.filter((x) => x.id !== id));
    } catch {
      alert('삭제에 실패했습니다.');
    } finally {
      setDeletingId(null);
    }
  };

  const signOut = () => { logout(); setLoggedIn(false); setUser(null); setGames([]); };

  if (!ready) return <Spinner text="불러오는 중..." />;

  const atLimit = games.length >= MAX_GAMES;

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-lg flex-col items-center justify-center gap-8 p-6">
      <div className="text-center">
        <h1 className="text-6xl font-black tracking-tight">FM Lite</h1>
        <p className="mt-4 text-zinc-400">
          팀을 선택하고, 전술을 짜고, 8팀 토너먼트 우승에 도전하세요.
        </p>
        {loggedIn && user && (
          <p className="mt-3 text-sm text-zinc-500">
            {user.email} ·{' '}
            <button onClick={signOut} className="text-zinc-400 hover:text-zinc-200 hover:underline">로그아웃</button>
          </p>
        )}
      </div>

      <div className="flex w-full flex-col gap-3">
        <Button onClick={startNewGame} disabled={loggedIn && atLimit} className="py-3 text-base">
          새 게임 시작
        </Button>
        {loggedIn && atLimit && (
          <p className="text-center text-xs text-amber-400">
            게임은 최대 {MAX_GAMES}개까지 만들 수 있습니다. 기존 게임을 삭제하세요.
          </p>
        )}

        {/* 로그인: 내 게임 목록 */}
        {loggedIn && games.length > 0 && (
          <div className="mt-2 flex flex-col gap-2">
            <p className="text-xs font-semibold text-zinc-500">내 게임 ({games.length}/{MAX_GAMES})</p>
            {games.map((g) => (
              <Card key={g.id}>
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="truncate font-semibold">{g.managerName} 감독</span>
                      <GradeBadge grade={g.teamGrade} label={statusLabel[g.status]} />
                    </div>
                    <p className="mt-1 truncate text-sm text-zinc-400">
                      {g.team.name} · {g.currentRoundLabel}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-1.5">
                    <button
                      onClick={() => { setSaveGameId(g.id); router.push(`/game/${g.id}`); }}
                      className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-emerald-500"
                    >
                      이어하기
                    </button>
                    <button
                      onClick={() => remove(g.id)}
                      disabled={deletingId === g.id}
                      className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm text-zinc-400 transition hover:border-red-500/50 hover:text-red-300 disabled:opacity-40"
                    >
                      {deletingId === g.id ? '삭제 중' : '삭제'}
                    </button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}

        {/* 비로그인: 로그인/회원가입 안내 */}
        {!loggedIn && (
          <div className="flex gap-2">
            <LinkButton href="/login" className="flex-1">로그인</LinkButton>
            <LinkButton href="/signup" className="flex-1">회원가입</LinkButton>
          </div>
        )}
      </div>

      <p className="text-xs text-zinc-600">가상 팀 · 가상 선수 · 규칙 기반 시뮬레이션</p>
    </main>
  );
}
