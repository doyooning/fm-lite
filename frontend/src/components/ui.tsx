'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import type { InputHTMLAttributes, ReactNode } from 'react';

export function Field({
  label, children, hint,
}: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-zinc-300">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-zinc-500">{hint}</span>}
    </label>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={`w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-emerald-500 focus:outline-none ${props.className ?? ''}`}
    />
  );
}

export function BackButton({ label = '뒤로', fallbackHref }: { label?: string; fallbackHref?: string }) {
  const router = useRouter();
  const goBack = () => {
    if (typeof window !== 'undefined' && window.history.length > 1) router.back();
    else if (fallbackHref) router.push(fallbackHref);
  };
  return (
    <button
      onClick={goBack}
      className="mb-3 inline-flex items-center gap-1 rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-sm text-zinc-300 transition hover:border-zinc-600 hover:text-zinc-100"
    >
      <span aria-hidden>←</span> {label}
    </button>
  );
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-zinc-800 bg-zinc-900/70 p-4 ${className}`}>
      {children}
    </div>
  );
}

const gradeColors: Record<string, string> = {
  STRONG: 'bg-red-500/15 text-red-400 border-red-500/30',
  UPPER_MID: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
  MID: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
  WEAK: 'bg-sky-500/15 text-sky-400 border-sky-500/30',
};

export function GradeBadge({ grade, label }: { grade: string; label: string }) {
  return (
    <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${gradeColors[grade] ?? 'bg-zinc-800 text-zinc-300 border-zinc-700'}`}>
      {label}
    </span>
  );
}

export function Button({
  children, onClick, disabled, variant = 'primary', className = '',
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
  className?: string;
}) {
  const styles = {
    primary: 'bg-emerald-600 hover:bg-emerald-500 text-white',
    secondary: 'bg-zinc-800 hover:bg-zinc-700 text-zinc-100 border border-zinc-700',
    danger: 'bg-red-600 hover:bg-red-500 text-white',
  }[variant];
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`rounded-lg px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40 ${styles} ${className}`}
    >
      {children}
    </button>
  );
}

export function LinkButton({
  href, children, variant = 'secondary', className = '',
}: {
  href: string;
  children: ReactNode;
  variant?: 'primary' | 'secondary';
  className?: string;
}) {
  const styles = variant === 'primary'
    ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
    : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-100 border border-zinc-700';
  return (
    <Link href={href} className={`inline-block rounded-lg px-4 py-2 text-center text-sm font-semibold transition ${styles} ${className}`}>
      {children}
    </Link>
  );
}

export function StatBar({ label, value, max = 99 }: { label: string; value: number; max?: number }) {
  const pct = Math.min(100, (value / max) * 100);
  const color = value >= 78 ? 'bg-emerald-500' : value >= 68 ? 'bg-amber-500' : 'bg-zinc-500';
  return (
    <div className="flex items-center gap-2">
      <span className="w-14 shrink-0 text-xs text-zinc-400">{label}</span>
      <div className="h-1.5 flex-1 rounded-full bg-zinc-800">
        <div className={`h-1.5 rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="w-6 shrink-0 text-right text-xs tabular-nums text-zinc-300">{value}</span>
    </div>
  );
}

export function Spinner({ text = '불러오는 중...' }: { text?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-zinc-400">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-600 border-t-emerald-500" />
      {text}
    </div>
  );
}

export function ErrorBox({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
      {message}
    </div>
  );
}
