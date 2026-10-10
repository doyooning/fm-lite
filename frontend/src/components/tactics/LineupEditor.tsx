'use client';

import { useMemo, useState } from 'react';
import type { Formation, Player } from '@/types/api';
import { FORMATION_LAYOUT, POSITION_LABEL, POSITION_ORDER, type Pos } from '@/lib/lineup';

const ROLE_STYLE: Record<Pos, { shirt: string; badge: string }> = {
  GK: { shirt: 'bg-yellow-400 text-zinc-900', badge: 'bg-yellow-500/20 text-yellow-300' },
  DF: { shirt: 'bg-sky-500 text-white', badge: 'bg-sky-500/20 text-sky-300' },
  MF: { shirt: 'bg-emerald-500 text-white', badge: 'bg-emerald-500/20 text-emerald-300' },
  FW: { shirt: 'bg-rose-500 text-white', badge: 'bg-rose-500/20 text-rose-300' },
};

function ovrColor(v: number) {
  if (v >= 85) return 'text-emerald-300';
  if (v >= 78) return 'text-lime-300';
  if (v >= 70) return 'text-amber-300';
  return 'text-zinc-300';
}

/**
 * FM 스타일 라인업 에디터.
 * - 왼쪽: 포메이션대로 배치된 피치. 슬롯은 선수의 본래 포지션과 무관하게 누구든 들어갈 수 있다.
 * - 오른쪽: 선수단 전체. 선수를 피치 슬롯으로 끌어다 놓으면 교체/자리 교환.
 * - 터치/클릭: 선수를 눌러 선택한 뒤 슬롯(또는 다른 선수)을 누르면 같은 동작.
 * lineup[i] 는 포메이션의 i번째 슬롯(GK → DF → MF → FW 순)에 서는 선수 id.
 */
export default function LineupEditor({
  formation, lineup, squad, onChange, stacked = false,
}: {
  formation: Formation;
  lineup: number[];
  squad: Player[];
  onChange: (lineup: number[]) => void;
  /** true 면 피치 아래에 선수단을 쌓는다 (좁은 영역용) */
  stacked?: boolean;
}) {
  const layout = FORMATION_LAYOUT[formation];
  const byId = useMemo(() => new Map(squad.map((p) => [p.id, p])), [squad]);
  const [dragId, setDragId] = useState<number | null>(null);
  const [overKey, setOverKey] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const slotOf = (id: number) => lineup.indexOf(id);

  /** 선수 src 를 슬롯 target 에 놓는다. src 가 이미 선발이면 자리 교환, 벤치면 교체 */
  const placeInSlot = (srcId: number, target: number) => {
    const next = [...lineup];
    const from = slotOf(srcId);
    if (from === target) return;
    if (from >= 0) {
      next[from] = lineup[target];
      next[target] = srcId;
    } else {
      next[target] = srcId;
    }
    onChange(next);
  };

  /** 선수 src 를 다른 선수 tgt 의 자리로 바꾼다 (선수단 목록 위에 떨어뜨렸을 때) */
  const swapPlayers = (srcId: number, tgtId: number) => {
    if (srcId === tgtId) return;
    const s = slotOf(srcId);
    const t = slotOf(tgtId);
    if (s < 0 && t < 0) return;
    const next = [...lineup];
    if (s >= 0 && t >= 0) { next[s] = tgtId; next[t] = srcId; }
    else if (s >= 0) next[s] = tgtId;
    else next[t] = srcId;
    onChange(next);
  };

  const clearDrag = () => { setDragId(null); setOverKey(null); };

  const onSlotClick = (index: number) => {
    const id = lineup[index];
    if (selectedId == null) { setSelectedId(id); return; }
    if (selectedId === id) { setSelectedId(null); return; }
    placeInSlot(selectedId, index);
    setSelectedId(null);
  };

  const onRowClick = (id: number) => {
    if (selectedId == null) { setSelectedId(id); return; }
    if (selectedId === id) { setSelectedId(null); return; }
    swapPlayers(selectedId, id);
    setSelectedId(null);
  };

  const dragStart = (e: React.DragEvent, id: number) => {
    e.dataTransfer.setData('text/plain', String(id));
    e.dataTransfer.effectAllowed = 'move';
    setDragId(id);
    setSelectedId(null);
  };

  const sortedSquad = useMemo(
    () => [...squad].sort((a, b) =>
      POSITION_ORDER.indexOf(a.position) - POSITION_ORDER.indexOf(b.position) || b.overall - a.overall),
    [squad],
  );
  const starters = sortedSquad.filter((p) => lineup.includes(p.id));
  const bench = sortedSquad.filter((p) => !lineup.includes(p.id));

  const renderRow = (p: Player) => {
    const slot = slotOf(p.id);
    const isStarter = slot >= 0;
    const role = isStarter ? layout[slot].role : null;
    const key = `row-${p.id}`;
    return (
      <div
        key={p.id}
        draggable
        onDragStart={(e) => dragStart(e, p.id)}
        onDragEnd={clearDrag}
        onDragOver={(e) => { if (dragId != null && dragId !== p.id) { e.preventDefault(); setOverKey(key); } }}
        onDragLeave={() => setOverKey((k) => (k === key ? null : k))}
        onDrop={(e) => { e.preventDefault(); if (dragId != null) swapPlayers(dragId, p.id); clearDrag(); }}
        onClick={() => onRowClick(p.id)}
        className={`flex cursor-grab items-center gap-2 rounded-lg border px-2 py-1.5 text-sm transition active:cursor-grabbing ${
          selectedId === p.id ? 'border-emerald-400 bg-emerald-500/15'
            : overKey === key ? 'border-emerald-400 bg-emerald-500/10'
              : isStarter ? 'border-zinc-700 bg-zinc-800/80' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
        } ${dragId === p.id ? 'opacity-40' : ''}`}
      >
        <span className={`w-8 shrink-0 rounded px-1 py-0.5 text-center text-[11px] font-bold ${ROLE_STYLE[p.position].badge}`}>
          {p.position}
        </span>
        <span className="min-w-0 flex-1 truncate font-medium">
          <span className="mr-1.5 text-xs tabular-nums text-zinc-500">{p.backNumber}</span>
          {p.name}
        </span>
        {isStarter && role && (
          <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${ROLE_STYLE[role].badge}`}>
            {role}{role !== p.position ? ' ⚠' : ''}
          </span>
        )}
        <span className={`w-7 shrink-0 text-right font-bold tabular-nums ${ovrColor(p.overall)}`}>{p.overall}</span>
      </div>
    );
  };

  return (
    <div className={stacked ? 'flex flex-col gap-3' : 'grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]'}>
      {/* 피치 */}
      <div
        className="relative mx-auto w-full max-w-[560px] select-none overflow-hidden rounded-xl border border-emerald-900/70"
        style={{
          aspectRatio: '68 / 86',
          backgroundColor: '#14532d',
          backgroundImage:
            'repeating-linear-gradient(to bottom, rgba(255,255,255,0.045) 0 11.11%, transparent 11.11% 22.22%)',
        }}
        onClick={() => setSelectedId(null)}
      >
        {/* 라인 마킹 */}
        <div className="pointer-events-none absolute inset-2 rounded-sm border border-white/25" />
        <div className="pointer-events-none absolute left-2 right-2 top-1/2 border-t border-white/25" />
        <div className="pointer-events-none absolute left-1/2 top-1/2 h-[18%] w-[22%] -translate-x-1/2 -translate-y-1/2 rounded-full border border-white/25" />
        <div className="pointer-events-none absolute left-1/2 top-2 h-[14%] w-[52%] -translate-x-1/2 border border-t-0 border-white/25" />
        <div className="pointer-events-none absolute bottom-2 left-1/2 h-[14%] w-[52%] -translate-x-1/2 border border-b-0 border-white/25" />
        <div className="pointer-events-none absolute left-1/2 top-2 h-[6%] w-[24%] -translate-x-1/2 border border-t-0 border-white/25" />
        <div className="pointer-events-none absolute bottom-2 left-1/2 h-[6%] w-[24%] -translate-x-1/2 border border-b-0 border-white/25" />

        {layout.map((spot, i) => {
          const p = byId.get(lineup[i]);
          const key = `slot-${i}`;
          const off = p != null && p.position !== spot.role;
          return (
            <div
              key={i}
              className="absolute z-10 w-[22%] -translate-x-1/2 -translate-y-1/2"
              style={{ left: `${spot.x}%`, top: `${spot.y}%` }}
              onDragOver={(e) => { if (dragId != null) { e.preventDefault(); setOverKey(key); } }}
              onDragLeave={() => setOverKey((k) => (k === key ? null : k))}
              onDrop={(e) => { e.preventDefault(); if (dragId != null) placeInSlot(dragId, i); clearDrag(); }}
              onClick={(e) => { e.stopPropagation(); onSlotClick(i); }}
            >
              <div
                draggable={p != null}
                onDragStart={(e) => p && dragStart(e, p.id)}
                onDragEnd={clearDrag}
                className={`flex cursor-grab flex-col items-center rounded-lg p-0.5 transition active:cursor-grabbing ${
                  overKey === key ? 'scale-110 bg-white/25 ring-2 ring-emerald-300'
                    : p && selectedId === p.id ? 'bg-emerald-400/30 ring-2 ring-emerald-300' : ''
                } ${p && dragId === p.id ? 'opacity-40' : ''}`}
              >
                <div
                  className={`flex h-9 w-9 items-center justify-center rounded-full border-2 border-white/80 text-sm font-extrabold shadow-md sm:h-10 sm:w-10 ${ROLE_STYLE[spot.role].shirt}`}
                >
                  {p ? p.backNumber : '?'}
                </div>
                <div className="mt-0.5 w-full max-w-[110px] overflow-hidden rounded bg-zinc-950/85 text-center leading-tight shadow">
                  <p className="truncate px-1 pt-0.5 text-[11px] font-semibold text-zinc-50 sm:text-xs">{p?.name ?? '비어 있음'}</p>
                  <p className="flex items-center justify-center gap-1 pb-0.5 text-[10px] text-zinc-400">
                    <span className={`rounded px-1 font-bold ${ROLE_STYLE[spot.role].badge}`}>{spot.role}</span>
                    {p && <span className={`font-bold tabular-nums ${ovrColor(p.overall)}`}>{p.overall}</span>}
                    {off && p && <span className="text-amber-400" title={`본래 포지션: ${POSITION_LABEL[p.position]}`}>⚠{p.position}</span>}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* 선수단 */}
      <div className={`flex flex-col gap-3 ${stacked ? '' : 'lg:max-h-[640px] lg:overflow-y-auto lg:pr-1'}`}>
        <p className="text-xs text-zinc-500">
          선수를 피치의 자리로 <b className="text-zinc-300">끌어다 놓거나</b>, 선수를 누른 뒤 자리를 누르세요.
          어느 선수든 어느 자리에든 배치할 수 있습니다. <span className="text-amber-400">⚠</span> 는 본래 포지션이 아닌 자리입니다.
        </p>
        <div>
          <h4 className="mb-1.5 text-xs font-semibold text-zinc-400">선발 ({starters.length})</h4>
          <div className="flex flex-col gap-1">{starters.map(renderRow)}</div>
        </div>
        <div>
          <h4 className="mb-1.5 text-xs font-semibold text-zinc-400">벤치 / 후보 ({bench.length})</h4>
          <div className="flex flex-col gap-1">{bench.map(renderRow)}</div>
        </div>
      </div>
    </div>
  );
}
