'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { saveGamesApi, teamsApi } from '@/lib/api';
import { isLoggedIn, setSaveGameId } from '@/lib/auth';
import { Button, Card, ErrorBox, Field, GradeBadge, Input, Spinner, StatBar } from '@/components/ui';
import type { TeamDetail, TeamSummary } from '@/types/api';

export default function NewGamePage() {
  const router = useRouter();
  const [step, setStep] = useState<'name' | 'team'>('name');
  const [managerName, setManagerName] = useState('');
  const [teams, setTeams] = useState<TeamSummary[] | null>(null);
  const [selected, setSelected] = useState<TeamDetail | null>(null);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    if (!isLoggedIn()) {
      router.replace('/login');
      return;
    }
    teamsApi.list().then(setTeams).catch((e) => setError(e.message));
  }, [router]);

  const toTeamStep = () => {
    if (!managerName.trim()) {
      setError('감독 이름을 입력해 주세요.');
      return;
    }
    setError('');
    setStep('team');
  };

  const pick = (teamId: number) =>
    teamsApi.detail(teamId).then(setSelected).catch((e) => setError(e.message));

  const start = async () => {
    if (!selected) return;
    setStarting(true);
    try {
      const save = await saveGamesApi.create(selected.id, managerName.trim());
      setSaveGameId(save.id);
      router.push(`/game/${save.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : '게임 생성 실패');
      setStarting(false);
    }
  };

  // 1단계: 감독 이름 설정
  if (step === 'name') {
    return (
      <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center p-6">
        <button onClick={() => router.push('/games')}
                className="mb-3 inline-flex items-center gap-1 self-start rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 transition hover:border-zinc-600">
          <span aria-hidden>←</span> 목록으로
        </button>
        <h1 className="text-2xl font-bold">감독 이름 설정</h1>
        <p className="mt-1 text-sm text-zinc-400">
          이 게임에서 사용할 이름입니다. 경기 중 지시가 이 이름으로 표시됩니다.
        </p>

        <Card className="mt-5">
          <Field label="감독 이름" hint="최대 30자">
            <Input value={managerName} maxLength={30} autoFocus
                   onChange={(e) => setManagerName(e.target.value)}
                   placeholder="예: 홍길동"
                   onKeyDown={(e) => { if (e.key === 'Enter') toTeamStep(); }} />
          </Field>
          {managerName.trim() && (
            <p className="mt-2 text-sm text-zinc-400">
              게임 내에서 <span className="font-semibold text-emerald-400">{managerName.trim()} 감독</span> 으로 불립니다.
            </p>
          )}
          {error && <div className="mt-3"><ErrorBox message={error} /></div>}
          <Button onClick={toTeamStep} className="mt-4 w-full py-2.5">다음: 팀 선택</Button>
        </Card>
      </main>
    );
  }

  // 2단계: 팀 선택
  return (
    <main className="mx-auto w-full max-w-4xl p-6">
      <button onClick={() => { setStep('name'); setError(''); }}
              className="mb-3 inline-flex items-center gap-1 rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 transition hover:border-zinc-600">
        <span aria-hidden>←</span> 감독 이름 다시 설정
      </button>
      <h1 className="text-2xl font-bold">팀 선택</h1>
      <p className="mt-1 text-sm text-zinc-400">
        <span className="font-semibold text-emerald-400">{managerName.trim()} 감독</span>이 지휘할 팀을 고르세요. 등급이 낮을수록 어려운 도전입니다.
      </p>

      {error && <div className="mt-4"><ErrorBox message={error} /></div>}
      {!teams && !error && <Spinner />}

      <div className="mt-6 grid gap-6 md:grid-cols-[1fr_320px]">
        <div className="grid gap-3 sm:grid-cols-2">
          {teams?.map((t) => (
            <button
              key={t.id}
              onClick={() => pick(t.id)}
              className={`rounded-xl border p-4 text-left transition hover:border-emerald-500/50 ${
                selected?.id === t.id ? 'border-emerald-500 bg-emerald-500/5' : 'border-zinc-800 bg-zinc-900/70'
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold">{t.name}</span>
                <GradeBadge grade={t.grade} label={t.gradeLabel} />
              </div>
              <p className="mt-2 line-clamp-2 text-xs text-zinc-400">{t.description}</p>
              <p className="mt-2 text-xs text-zinc-500">평균 능력치 <span className="font-semibold text-zinc-300">{t.avgRating}</span></p>
            </button>
          ))}
        </div>

        <div>
          {selected ? (
            <Card className="sticky top-6">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold">{selected.name}</h2>
                <GradeBadge grade={selected.grade} label={selected.gradeLabel} />
              </div>
              <p className="mt-1 text-sm text-zinc-400">{selected.description}</p>

              <div className="mt-4 flex flex-col gap-1.5">
                <StatBar label="공격" value={selected.powerByArea.attack} />
                <StatBar label="중원" value={selected.powerByArea.midfield} />
                <StatBar label="수비" value={selected.powerByArea.defense} />
                <StatBar label="GK" value={selected.powerByArea.goalkeeping} />
              </div>

              <h3 className="mt-4 text-xs font-semibold text-zinc-500">주요 선수</h3>
              <ul className="mt-1 text-sm">
                {selected.keyPlayers.map((p) => (
                  <li key={p.id} className="flex justify-between py-0.5">
                    <span>{p.name} <span className="text-xs text-zinc-500">{p.position}</span></span>
                    <span className="tabular-nums text-zinc-300">{p.overall}</span>
                  </li>
                ))}
              </ul>

              <Button onClick={start} disabled={starting} className="mt-4 w-full py-2.5">
                {starting ? '대회 준비 중...' : '이 팀으로 게임 시작'}
              </Button>
            </Card>
          ) : (
            <Card className="text-center text-sm text-zinc-500">팀을 선택하면 상세 정보가 표시됩니다.</Card>
          )}
        </div>
      </div>
    </main>
  );
}
