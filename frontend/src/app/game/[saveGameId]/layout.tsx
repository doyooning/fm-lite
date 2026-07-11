'use client';

import Link from 'next/link';
import { useParams, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { saveGamesApi } from '@/lib/api';
import { GradeBadge } from '@/components/ui';
import type { SaveGame } from '@/types/api';

export default function GameLayout({ children }: { children: React.ReactNode }) {
  const { saveGameId } = useParams<{ saveGameId: string }>();
  const pathname = usePathname();
  const [saveGame, setSaveGame] = useState<SaveGame | null>(null);

  useEffect(() => {
    saveGamesApi.get(Number(saveGameId)).then(setSaveGame).catch(() => {});
  }, [saveGameId, pathname]);

  const base = `/game/${saveGameId}`;
  const nav = [
    { href: base, label: '허브' },
    { href: `${base}/squad`, label: '선수단' },
    { href: `${base}/competition`, label: '대진표' },
  ];

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-zinc-800 bg-zinc-950/90 backdrop-blur">
        <div className="mx-auto flex w-full max-w-4xl items-center justify-between gap-3 px-6 py-3">
          <Link href="/" className="text-sm font-black tracking-tight text-emerald-500">FM Lite</Link>
          {saveGame && (
            <div className="flex items-center gap-2 text-sm">
              <span className="font-semibold">{saveGame.team.name}</span>
              <GradeBadge grade={saveGame.teamGrade} label={saveGame.currentRoundLabel} />
            </div>
          )}
          <nav className="flex gap-1 text-sm">
            {nav.map((n) => (
              <Link
                key={n.href}
                href={n.href}
                className={`rounded-md px-2.5 py-1 transition ${
                  pathname === n.href ? 'bg-zinc-800 text-zinc-100' : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {n.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>
      <div className="flex-1">{children}</div>
    </div>
  );
}
