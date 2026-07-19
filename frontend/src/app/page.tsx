'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { isLoggedIn, logout } from '@/lib/auth';
import { Button, LinkButton } from '@/components/ui';

export default function HomePage() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
  }, []);

  const createGame = () => {
    router.push(isLoggedIn() ? '/games' : '/login');
  };

  const signOut = () => { logout(); setLoggedIn(false); };

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-lg flex-col items-center justify-center gap-8 p-6">
      <div className="text-center">
        <h1 className="text-6xl font-black tracking-tight">FM Lite</h1>
        <p className="mt-4 text-zinc-400">
          팀을 선택하고, 전술을 짜고, 8팀 토너먼트 우승에 도전하세요.
        </p>
      </div>

      <div className="flex w-full flex-col gap-3">
        <Button onClick={createGame} className="py-3 text-base">
          게임 생성
        </Button>

        {loggedIn ? (
          <p className="text-center text-sm text-zinc-500">
            <button onClick={signOut} className="text-zinc-400 hover:text-zinc-200 hover:underline">로그아웃</button>
          </p>
        ) : (
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
