'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { matchesApi, saveGamesApi, teamsApi } from '@/lib/api';
import { BackButton, Button, Card, ErrorBox, Spinner } from '@/components/ui';
import TacticForm from '@/components/tactics/TacticForm';
import { isValidLineup } from '@/lib/lineup';
import type { Player, Tactic } from '@/types/api';

export default function TacticsPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const id = Number(saveGameId);
  const router = useRouter();
  const [matchId, setMatchId] = useState<number | null>(null);
  const [tactic, setTactic] = useState<Tactic | null>(null);
  const [squad, setSquad] = useState<Player[]>([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    saveGamesApi.nextMatch(id)
      .then(async (next) => {
        if (!next.hasNext || !next.match) throw new Error('전술을 설정할 다음 경기가 없습니다.');
        const m = next.match;
        setMatchId(m.matchId);
        const userTeamId = m.isUserHome ? m.homeTeam.id : m.awayTeam.id;
        const [tac, players] = await Promise.all([
          matchesApi.getMyTactic(m.matchId),
          teamsApi.players(userTeamId),
        ]);
        setSquad(players);
        setTactic(tac.tactic);
      })
      .catch((e) => setError(e.message));
  }, [id]);

  const save = async (thenStart: boolean) => {
    if (!matchId || !tactic) return;
    if (!isValidLineup(tactic.lineup, squad, tactic.formation)) {
      setError('선발 라인업이 포메이션 인원과 맞지 않습니다.');
      return;
    }
    setSaving(true);
    try {
      await matchesApi.saveMyTactic(matchId, tactic);
      router.push(thenStart ? `/game/${id}/match/${matchId}` : `/game/${id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장 실패');
      setSaving(false);
    }
  };

  if (error) return <main className="mx-auto max-w-3xl p-6"><BackButton fallbackHref={`/game/${id}`} /><ErrorBox message={error} /></main>;
  if (!tactic) return <Spinner />;

  return (
    <main className="mx-auto w-full max-w-3xl p-6">
      <BackButton fallbackHref={`/game/${id}`} />
      <h1 className="text-2xl font-bold">경기 전 전술 설정</h1>
      <p className="mt-1 text-sm text-zinc-400">
        킥오프 전 마지막 준비입니다. 선발은 베스트 XI로 맞춰져 있고, 원하는 선수로 교체할 수 있습니다.
      </p>

      <Card className="mt-5">
        <TacticForm value={tactic} squad={squad} onChange={setTactic} />
      </Card>

      <div className="mt-6 flex justify-end gap-2">
        <Button variant="secondary" onClick={() => save(false)} disabled={saving}>저장</Button>
        <Button onClick={() => save(true)} disabled={saving}>
          {saving ? '저장 중...' : '저장 후 경기 시작'}
        </Button>
      </div>
    </main>
  );
}
