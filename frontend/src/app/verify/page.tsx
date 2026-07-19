'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { authApi } from '@/lib/api';
import { Card, Spinner } from '@/components/ui';

type State = 'verifying' | 'success' | 'error';

export default function VerifyPage() {
  const [state, setState] = useState<State>('verifying');
  const [message, setMessage] = useState('');
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;
    // useSearchParams 대신 window 에서 직접 읽어 Suspense 요구를 피한다
    const token = new URLSearchParams(window.location.search).get('token');
    if (!token) {
      setState('error');
      setMessage('인증 토큰이 없습니다. 메일의 링크를 다시 확인해 주세요.');
      return;
    }
    authApi.verify(token)
      .then(() => setState('success'))
      .catch((e) => {
        setState('error');
        setMessage(e instanceof Error ? e.message : '인증에 실패했습니다.');
      });
  }, []);

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-sm flex-col justify-center gap-6 p-6">
      {state === 'verifying' && <Spinner text="이메일 인증 중..." />}

      {state === 'success' && (
        <Card className="border-emerald-500/40 bg-emerald-500/10 text-center">
          <p className="text-3xl">✅</p>
          <h1 className="mt-2 text-lg font-bold text-emerald-300">이메일 인증 완료!</h1>
          <p className="mt-2 text-sm text-zinc-300">이제 로그인하여 게임을 시작할 수 있습니다.</p>
          <Link href="/login"
                className="mt-4 inline-block rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500">
            로그인하러 가기
          </Link>
        </Card>
      )}

      {state === 'error' && (
        <Card className="border-red-500/30 bg-red-500/10 text-center">
          <p className="text-3xl">⚠️</p>
          <h1 className="mt-2 text-lg font-bold text-red-300">인증 실패</h1>
          <p className="mt-2 text-sm text-zinc-300">{message}</p>
          <Link href="/login"
                className="mt-4 inline-block rounded-lg border border-zinc-700 px-4 py-2 text-sm font-semibold text-zinc-200 hover:border-zinc-500">
            로그인 화면으로
          </Link>
        </Card>
      )}
    </main>
  );
}
