'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api';
import { setToken } from '@/lib/auth';
import { ApiError } from '@/lib/api/client';
import { Button, Card, ErrorBox, Field, Input } from '@/components/ui';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [needsVerify, setNeedsVerify] = useState(false);
  const [busy, setBusy] = useState(false);
  const [resent, setResent] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setNeedsVerify(false);
    setBusy(true);
    try {
      const { token } = await authApi.login(email, password);
      setToken(token);
      router.push('/');
    } catch (err) {
      if (err instanceof ApiError && err.code === 'EMAIL_NOT_VERIFIED') {
        setNeedsVerify(true);
        setError(err.message);
      } else {
        setError(err instanceof Error ? err.message : '로그인에 실패했습니다.');
      }
      setBusy(false);
    }
  };

  const resend = async () => {
    try {
      await authApi.resend(email);
      setResent(true);
    } catch {
      /* 조용히 무시 */
    }
  };

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-sm flex-col justify-center gap-6 p-6">
      <div className="text-center">
        <h1 className="text-4xl font-black tracking-tight">FM Lite</h1>
        <p className="mt-2 text-sm text-zinc-400">로그인하고 우승에 도전하세요.</p>
      </div>

      <Card>
        <form onSubmit={submit} className="flex flex-col gap-4">
          <Field label="이메일">
            <Input type="email" autoComplete="email" required value={email}
                   onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </Field>
          <Field label="비밀번호">
            <Input type="password" autoComplete="current-password" required value={password}
                   onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </Field>

          {error && <ErrorBox message={error} />}
          {needsVerify && (
            <button type="button" onClick={resend}
                    className="text-left text-xs text-emerald-400 hover:underline">
              {resent ? '인증 메일을 다시 보냈습니다. 메일함을 확인하세요.' : '인증 메일 다시 받기'}
            </button>
          )}

          <Button onClick={() => {}} disabled={busy} className="w-full py-2.5">
            {busy ? '로그인 중...' : '로그인'}
          </Button>
        </form>
      </Card>

      <p className="text-center text-sm text-zinc-400">
        계정이 없으신가요?{' '}
        <Link href="/signup" className="font-semibold text-emerald-400 hover:underline">회원가입</Link>
      </p>
    </main>
  );
}
