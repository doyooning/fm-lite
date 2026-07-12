'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { saveGamesApi, teamsApi } from '@/lib/api';
import { BackButton, Card, ErrorBox, Spinner } from '@/components/ui';
import type { Player } from '@/types/api';

const POSITIONS = ['GK', 'DF', 'MF', 'FW'] as const;
const posLabel: Record<string, string> = { GK: '골키퍼', DF: '수비수', MF: '미드필더', FW: '공격수' };

const statCols: { key: keyof Player['stats']; label: string }[] = [
  { key: 'attack', label: '공격' },
  { key: 'defense', label: '수비' },
  { key: 'passing', label: '패스' },
  { key: 'speed', label: '스피드' },
  { key: 'stamina', label: '체력' },
  { key: 'mentality', label: '멘탈' },
  { key: 'finishing', label: '결정력' },
  { key: 'goalkeeping', label: 'GK' },
];

function statColor(v: number) {
  return v >= 78 ? 'text-emerald-400' : v >= 68 ? 'text-amber-400' : 'text-zinc-500';
}

export default function SquadPage() {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const [players, setPlayers] = useState<Player[] | null>(null);
  const [teamName, setTeamName] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    saveGamesApi.get(Number(saveGameId))
      .then((sg) => {
        setTeamName(sg.team.name);
        return teamsApi.players(sg.team.id);
      })
      .then(setPlayers)
      .catch((e) => setError(e.message));
  }, [saveGameId]);

  if (error) return <main className="mx-auto max-w-4xl p-6"><ErrorBox message={error} /></main>;
  if (!players) return <Spinner />;

  return (
    <main className="mx-auto w-full max-w-4xl p-6">
      <BackButton fallbackHref={`/game/${saveGameId}`} />
      <h1 className="text-2xl font-bold">{teamName} 선수단</h1>
      <p className="mt-1 text-sm text-zinc-400">GK 2 · DF 6 · MF 6 · FW 4 — 경기마다 포메이션에 맞춰 베스트 XI가 자동 선발됩니다.</p>

      {POSITIONS.map((pos) => (
        <Card key={pos} className="mt-4 overflow-x-auto p-0">
          <h2 className="border-b border-zinc-800 px-4 py-2 text-sm font-semibold text-zinc-300">
            {posLabel[pos]}
          </h2>
          <table className="w-full min-w-[720px] text-sm">
            <thead>
              <tr className="text-left text-xs text-zinc-500">
                <th className="px-4 py-2 font-medium">번호</th>
                <th className="py-2 font-medium">이름</th>
                <th className="py-2 text-center font-medium">종합</th>
                {statCols.map((c) => (
                  <th key={c.key} className="py-2 text-center font-medium">{c.label}</th>
                ))}
                <th className="py-2 font-medium">특성</th>
              </tr>
            </thead>
            <tbody>
              {players.filter((p) => p.position === pos)
                .sort((a, b) => b.overall - a.overall)
                .map((p) => (
                  <tr key={p.id} className="border-t border-zinc-800/60">
                    <td className="px-4 py-2 tabular-nums text-zinc-500">{p.backNumber}</td>
                    <td className="py-2 font-medium">{p.name}</td>
                    <td className="py-2 text-center font-bold tabular-nums text-emerald-400">{p.overall}</td>
                    {statCols.map((c) => (
                      <td key={c.key} className={`py-2 text-center tabular-nums ${statColor(p.stats[c.key])}`}>
                        {p.stats[c.key]}
                      </td>
                    ))}
                    <td className="py-2 pr-4">
                      <div className="flex flex-wrap gap-1">
                        {p.traits.map((t) => (
                          <span key={t.code} title={t.description}
                                className={`cursor-help rounded border px-1.5 py-0.5 text-xs ${
                                  t.positive
                                    ? 'border-sky-500/40 bg-sky-500/15 text-sky-300'
                                    : 'border-red-500/40 bg-red-500/15 text-red-300'
                                }`}>
                            {t.name}
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </Card>
      ))}
    </main>
  );
}
