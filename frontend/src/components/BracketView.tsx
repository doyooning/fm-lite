import type { Bracket, BracketMatch } from '@/types/api';

function MatchBox({ m, userTeamId }: { m: BracketMatch; userTeamId: number | null }) {
  const line = (teamId: number, name: string, score: number | null, pen: number | null) => {
    const winner = m.winnerTeamId === teamId;
    const mine = userTeamId === teamId;
    return (
      <div className={`flex items-center justify-between gap-2 ${winner ? 'text-emerald-400 font-semibold' : 'text-zinc-300'}`}>
        <span className="truncate">
          {mine && <span className="mr-1 text-amber-400">★</span>}
          {name}
        </span>
        <span className="tabular-nums">
          {score ?? '-'}
          {pen !== null && <span className="ml-1 text-xs text-zinc-500">({pen})</span>}
        </span>
      </div>
    );
  };
  return (
    <div className={`rounded-lg border p-2.5 text-sm ${m.isUserMatch ? 'border-amber-500/40 bg-amber-500/5' : 'border-zinc-800 bg-zinc-900/60'}`}>
      {line(m.homeTeam.id, m.homeTeam.name, m.homeScore, m.penaltyHomeScore)}
      {line(m.awayTeam.id, m.awayTeam.name, m.awayScore, m.penaltyAwayScore)}
    </div>
  );
}

export default function BracketView({ bracket, userTeamId }: { bracket: Bracket; userTeamId: number | null }) {
  return (
    <div className="grid gap-4 md:grid-cols-3">
      {bracket.rounds.map((r) => (
        <div key={r.round}>
          <h3 className="mb-2 text-sm font-semibold text-zinc-400">{r.roundLabel}</h3>
          <div className="flex flex-col justify-around gap-3 md:h-[calc(100%-2rem)]">
            {r.matches.length === 0 && (
              <div className="rounded-lg border border-dashed border-zinc-800 p-3 text-center text-xs text-zinc-600">
                이전 라운드 진행 후 확정
              </div>
            )}
            {r.matches.map((m) => (
              <MatchBox key={m.matchId} m={m} userTeamId={userTeamId} />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
