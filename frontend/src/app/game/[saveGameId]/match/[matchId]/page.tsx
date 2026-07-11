'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { matchesApi } from '@/lib/api';
import { Card, ErrorBox, Spinner } from '@/components/ui';
import type { MatchEvent, MatchInfo, MatchStatus } from '@/types/api';

const REVEAL_INTERVAL_MS = 750;

const eventIcon: Record<MatchEvent['eventType'], string> = {
  KICK_OFF: '🟢', CHANCE: '⚡', GOAL: '⚽', SAVE: '🧤', MISS: '💨',
  HALF_TIME: '⏸', TACTIC_CHANGE: '🔄', CHOICE: '❓', COACH_DECISION: '📣',
  FULL_TIME: '🏁', PENALTY_SHOOTOUT: '🥅',
};

function eventStyle(type: MatchEvent['eventType']) {
  switch (type) {
    case 'GOAL': return 'border-emerald-500/40 bg-emerald-500/10 font-semibold';
    case 'SAVE': return 'border-sky-500/30 bg-sky-500/5';
    case 'TACTIC_CHANGE': return 'border-amber-500/30 bg-amber-500/5 text-amber-200';
    case 'COACH_DECISION': return 'border-violet-500/30 bg-violet-500/5 text-violet-200';
    case 'FULL_TIME': case 'PENALTY_SHOOTOUT': return 'border-zinc-600 bg-zinc-800/80 font-semibold';
    default: return 'border-zinc-800 bg-zinc-900/60';
  }
}

export default function MatchLivePage() {
  const { saveGameId, matchId } = useParams<{ saveGameId: string; matchId: string }>();
  const mid = Number(matchId);
  const router = useRouter();

  const [info, setInfo] = useState<MatchInfo | null>(null);
  const [revealed, setRevealed] = useState<MatchEvent[]>([]);
  const [queue, setQueue] = useState<MatchEvent[]>([]);
  const [status, setStatus] = useState<MatchStatus>('SCHEDULED');
  const [pendingChoice, setPendingChoice] = useState<MatchEvent | null>(null);
  const [choosing, setChoosing] = useState(false);
  const [error, setError] = useState('');
  const didInit = useRef(false);
  const feedEndRef = useRef<HTMLDivElement>(null);

  // 최초 로딩: SCHEDULED면 시작, 아니면 로그 복구
  useEffect(() => {
    if (didInit.current) return;
    didInit.current = true;
    (async () => {
      try {
        const i = await matchesApi.info(mid);
        setInfo(i);
        if (i.status === 'SCHEDULED') {
          const p = await matchesApi.start(mid);
          setStatus(p.matchStatus);
          setQueue(p.events);
        } else {
          const p = await matchesApi.events(mid);
          setStatus(p.matchStatus);
          // 새로고침 복구: 지난 이벤트는 즉시 표시
          const answered = p.events.filter((e) => !e.requiresChoice || e.selectedChoiceId !== null);
          const pending = p.events.find((e) => e.requiresChoice && e.selectedChoiceId === null);
          setRevealed(answered);
          if (pending) setPendingChoice(pending);
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : '경기를 불러오지 못했습니다.');
      }
    })();
  }, [mid]);

  // 이벤트 순차 공개 (seq 기준 중복 방지 — StrictMode/멱등 재시작 대응)
  useEffect(() => {
    if (queue.length === 0) return;
    const timer = setTimeout(() => {
      const [head, ...rest] = queue;
      setRevealed((r) => (r.some((e) => e.seq === head.seq) ? r : [...r, head]));
      if (head.requiresChoice && head.selectedChoiceId === null) setPendingChoice(head);
      setQueue(rest);
    }, REVEAL_INTERVAL_MS);
    return () => clearTimeout(timer);
  }, [queue]);

  // 종료 시 결과 페이지로
  useEffect(() => {
    if (status === 'FINISHED' && queue.length === 0 && revealed.length > 0) {
      const t = setTimeout(() => router.push(`/game/${saveGameId}/match/${mid}/result`), 1500);
      return () => clearTimeout(t);
    }
  }, [status, queue.length, revealed.length, router, saveGameId, mid]);

  useEffect(() => {
    feedEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [revealed.length]);

  const choose = useCallback(async (choiceId: string) => {
    if (!pendingChoice || choosing) return;
    setChoosing(true);
    try {
      const p = await matchesApi.choose(mid, pendingChoice.eventId, choiceId);
      setPendingChoice(null);
      setStatus(p.matchStatus);
      setQueue((q) => [...q, ...p.events]);
    } catch (e) {
      setError(e instanceof Error ? e.message : '선택 처리 실패');
    } finally {
      setChoosing(false);
    }
  }, [mid, pendingChoice, choosing]);

  if (error) return <main className="mx-auto max-w-2xl p-6"><ErrorBox message={error} /></main>;
  if (!info) return <Spinner text="경기장 입장 중..." />;

  const homeGoals = revealed.filter((e) => e.eventType === 'GOAL' && e.teamId === info.homeTeam.id).length;
  const awayGoals = revealed.filter((e) => e.eventType === 'GOAL' && e.teamId === info.awayTeam.id).length;
  const lastMinute = revealed.length ? revealed[revealed.length - 1].minute : 0;
  const live = queue.length > 0 || status === 'IN_PROGRESS' || status === 'WAITING_CHOICE';

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col p-6">
      {/* 스코어보드 */}
      <Card className="sticky top-0 z-10 bg-zinc-900">
        <p className="text-center text-xs font-semibold text-emerald-500">{info.roundLabel}</p>
        <div className="mt-1 flex items-center justify-center gap-4">
          <span className={`flex-1 text-right text-lg font-bold ${info.isUserHome ? 'text-amber-300' : ''}`}>
            {info.homeTeam.name}
          </span>
          <span className="rounded-lg bg-zinc-800 px-4 py-1.5 text-2xl font-black tabular-nums">
            {homeGoals} : {awayGoals}
          </span>
          <span className={`flex-1 text-lg font-bold ${!info.isUserHome ? 'text-amber-300' : ''}`}>
            {info.awayTeam.name}
          </span>
        </div>
        <div className="mt-3 h-1.5 w-full rounded-full bg-zinc-800">
          <div className="h-1.5 rounded-full bg-emerald-500 transition-all duration-700"
               style={{ width: `${Math.min(100, (lastMinute / 90) * 100)}%` }} />
        </div>
        <p className="mt-1 text-center text-xs tabular-nums text-zinc-500">
          {status === 'FINISHED' && queue.length === 0 ? '경기 종료' : `${lastMinute}'`}
        </p>
      </Card>

      {/* 중계 피드 */}
      <div className="mt-4 flex flex-col gap-2">
        {revealed.map((e) => (
          <div key={e.seq}
               className={`flex items-start gap-2.5 rounded-lg border px-3 py-2 text-sm ${eventStyle(e.eventType)}`}>
            <span className="mt-0.5">{eventIcon[e.eventType]}</span>
            <div>
              <span className="mr-2 text-xs tabular-nums text-zinc-500">{e.minute}&apos;</span>
              {e.description}
            </div>
          </div>
        ))}
        {live && !pendingChoice && (
          <p className="animate-pulse py-2 text-center text-xs text-zinc-500">경기 진행 중...</p>
        )}
        <div ref={feedEndRef} />
      </div>

      {/* 선택지 */}
      {pendingChoice && queue.length === 0 && (
        <Card className="mt-4 border-violet-500/40 bg-violet-500/5">
          <p className="text-sm font-semibold text-violet-200">📣 {pendingChoice.description}</p>
          <div className="mt-3 grid gap-2">
            {pendingChoice.choiceOptions?.map((o) => (
              <button key={o.id} onClick={() => choose(o.id)} disabled={choosing}
                      className="rounded-lg border border-zinc-700 bg-zinc-900 p-3 text-left transition hover:border-violet-400 disabled:opacity-40">
                <p className="font-semibold">{o.label}</p>
                <p className="mt-0.5 text-xs text-zinc-400">{o.description}</p>
              </button>
            ))}
          </div>
        </Card>
      )}

      {status === 'FINISHED' && queue.length === 0 && (
        <p className="mt-4 text-center text-sm text-zinc-400">결과 페이지로 이동합니다...</p>
      )}
    </main>
  );
}
