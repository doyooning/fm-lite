'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { profileApi } from '@/lib/api';
import { isLoggedIn, logout } from '@/lib/auth';
import { Card, ErrorBox, Spinner } from '@/components/ui';
import type { Profile } from '@/types/api';

function Stat({ label, value, accent }: { label: string; value: number | string; accent?: string }) {
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/70 p-3 text-center">
      <p className={`text-2xl font-black tabular-nums ${accent ?? 'text-zinc-100'}`}>{value}</p>
      <p className="mt-0.5 text-xs text-zinc-500">{label}</p>
    </div>
  );
}

export default function ProfilePage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoggedIn()) {
      router.replace('/login');
      return;
    }
    profileApi.me().then(setProfile).catch((e) => setError(e.message));
  }, [router]);

  const signOut = () => { logout(); router.replace('/login'); };

  if (error) return <main className="mx-auto max-w-lg p-6"><ErrorBox message={error} /></main>;
  if (!profile) return <Spinner />;

  const achievedCount = profile.achievements.filter((a) => a.achieved).length;

  return (
    <main className="mx-auto w-full max-w-lg p-6">
      <div className="mb-5 flex items-center justify-between">
        <Link href="/games" className="text-lg font-black tracking-tight text-emerald-500">FM Lite</Link>
        <button onClick={signOut} className="text-sm text-zinc-400 hover:text-zinc-200 hover:underline">
          로그아웃
        </button>
      </div>

      <h1 className="text-2xl font-bold">프로필</h1>
      <p className="mt-1 text-sm text-zinc-400">{profile.email}</p>
      <p className="mt-0.5 text-xs text-zinc-600">
        가입일 {new Date(profile.joinedAt).toLocaleDateString('ko-KR')}
      </p>

      {/* 통산 우승 */}
      <Card className="mt-5 border-amber-500/30 bg-amber-500/5 text-center">
        <p className="text-4xl">🏆</p>
        <p className="mt-1 text-3xl font-black tabular-nums text-amber-300">{profile.championships}</p>
        <p className="mt-0.5 text-sm text-zinc-400">통산 우승</p>
      </Card>

      {/* 게임 통계 */}
      <div className="mt-4 grid grid-cols-4 gap-2">
        <Stat label="전체" value={profile.games.total} />
        <Stat label="진행 중" value={profile.games.inProgress} accent="text-emerald-400" />
        <Stat label="우승" value={profile.games.champion} accent="text-amber-400" />
        <Stat label="탈락" value={profile.games.eliminated} accent="text-zinc-500" />
      </div>

      {/* 업적 */}
      <div className="mt-6 flex items-baseline justify-between">
        <h2 className="text-lg font-bold">업적</h2>
        <span className="text-sm text-zinc-500">{achievedCount}/{profile.achievements.length}</span>
      </div>
      <div className="mt-2 flex flex-col gap-2">
        {profile.achievements.map((a) => (
          <Card key={a.code}
                className={a.achieved ? 'border-emerald-500/40 bg-emerald-500/5' : 'opacity-60'}>
            <div className="flex items-start gap-3">
              <span className={`text-2xl ${a.achieved ? '' : 'grayscale'}`}>{a.icon}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className={`font-semibold ${a.achieved ? 'text-emerald-300' : 'text-zinc-400'}`}>
                    {a.label}
                  </span>
                  {a.achieved && <span className="text-xs text-emerald-500">달성</span>}
                </div>
                <p className="mt-0.5 text-xs text-zinc-500">{a.description}</p>
                {a.achieved && a.achievedAt && (
                  <p className="mt-1 text-xs text-zinc-600">
                    {new Date(a.achievedAt).toLocaleDateString('ko-KR')} 달성
                  </p>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>

      <div className="mt-6">
        <Link href="/games"
              className="block rounded-lg border border-zinc-700 py-2.5 text-center text-sm font-semibold text-zinc-200 hover:border-zinc-500">
          내 게임으로
        </Link>
      </div>
    </main>
  );
}
