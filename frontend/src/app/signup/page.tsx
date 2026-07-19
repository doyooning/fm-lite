'use client';

import { useState } from 'react';
import Link from 'next/link';
import { authApi } from '@/lib/api';
import { Button, Card, ErrorBox, Field, Input } from '@/components/ui';

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    setBusy(true);
    try {
      await authApi.register(email, password, nickname || undefined);
      setDone(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
      setBusy(false);
    }
  };

  if (done) {
    return (
      <main className="mx-auto flex min-h-screen w-full max-w-sm flex-col justify-center gap-6 p-6">
        <Card className="text-center">
          <p className="text-3xl">📧</p>
          <h1 className="mt-2 text-lg font-bold">인증 메일을 확인하세요</h1>
          <p className="mt-2 text-sm text-zinc-400">
            <span className="font-semibold text-zinc-200">{email}</span> 로 인증 링크를 보냈습니다.
            메일의 링크를 눌러 인증을 완료한 뒤 로그인하세요.
          </p>
          <Link href="/login"
                className="mt-4 inline-block rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500">
            로그인하러 가기
          </Link>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-sm flex-col justify-center gap-6 p-6">
      <div className="text-center">
        <h1 className="text-4xl font-black tracking-tight">회원가입</h1>
        <p className="mt-2 text-sm text-zinc-400">이메일로 계정을 만드세요.</p>
      </div>

      <Card>
        <form onSubmit={submit} className="flex flex-col gap-4">
          <Field label="이메일">
            <Input type="email" autoComplete="email" required value={email}
                   onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </Field>
          <Field label="비밀번호" hint="8자 이상">
            <Input type="password" autoComplete="new-password" required value={password}
                   onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </Field>
          <Field label="닉네임" hint="선택 · 비우면 '감독'">
            <Input type="text" value={nickname} maxLength={30}
                   onChange={(e) => setNickname(e.target.value)} placeholder="감독" />
          </Field>

          {error && <ErrorBox message={error} />}

          <Button disabled={busy} className="w-full py-2.5">
            {busy ? '가입 중...' : '회원가입'}
          </Button>
        </form>
      </Card>

      <p className="text-center text-sm text-zinc-400">
        이미 계정이 있으신가요?{' '}
        <Link href="/login" className="font-semibold text-emerald-400 hover:underline">로그인</Link>
      </p>
    </main>
  );
}
