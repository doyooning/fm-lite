'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi, saveGamesApi } from '@/lib/api';
import { isLoggedIn, logout, setSaveGameId } from '@/lib/auth';
import { Button, Card, GradeBadge, Spinner } from '@/components/ui';
import type { SaveGame, User } from '@/types/api';

const MAX_GAMES = 3;
const statusLabel: Record<SaveGame['status'], string> = {
  IN_PROGRESS: '진행 중',
  CHAMPION: '🏆 우승',
  ELIMINATED: '탈락',
};

export default function GamesPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [games, setGames] = useState<SaveGame[]>([]);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    if (!isLoggedIn()) {
      router.replace('/login');
      return;
    }
    authApi.me()
      .then((me) => { setUser(me); return saveGamesApi.listMine(); })
      .then((list) => setGames(list))
      .catch(() => {})
      .finally(() => setReady(true));
  }, [router]);

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

  const signOut = () => { logout(); router.replace('/login'); };

  if (!ready) return <Spinner text="불러오는 중..." />;

  const atLimit = games.length >= MAX_GAMES;

  return (
    <main className="mx-auto w-full max-w-lg p-6">
      <div className="mb-5 flex items-center justify-between">
        <Link href="/" className="text-lg font-black tracking-tight text-emerald-500">FM Lite</Link>
        <span className="text-sm text-zinc-500">
          <Link href="/profile" className="text-zinc-300 hover:text-emerald-400 hover:underline">프로필</Link>
          {' · '}{user?.email}{' · '}
          <button onClick={signOut} className="text-zinc-400 hover:text-zinc-200 hover:underline">로그아웃</button>
        </span>
      </div>

      <div className="mb-3 flex items-baseline justify-between">
        <h1 className="text-2xl font-bold">내 게임</h1>
        <span className="text-sm text-zinc-500">{games.length}/{MAX_GAMES}</span>
      </div>

      {games.length === 0 ? (
        <Card className="text-center text-sm text-zinc-400">
          아직 생성한 게임이 없습니다. 아래에서 새 게임을 만들어 보세요.
        </Card>
      ) : (
        <div className="flex flex-col gap-2">
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

      <div className="mt-5">
        <Button onClick={() => router.push('/new-game')} disabled={atLimit} className="w-full py-3">
          + 새 게임 생성
        </Button>
        {atLimit && (
          <p className="mt-2 text-center text-xs text-amber-400">
            게임은 최대 {MAX_GAMES}개까지 만들 수 있습니다. 기존 게임을 삭제하세요.
          </p>
        )}
      </div>
    </main>
  );
}
