import { api } from '@/lib/api/client';
import type { User } from '@/types/api';

const USER_KEY = 'fmlite.userId';
const SAVE_KEY = 'fmlite.saveGameId';

export function getUserId(): string | null {
  return typeof window === 'undefined' ? null : localStorage.getItem(USER_KEY);
}

export function getSaveGameId(): number | null {
  if (typeof window === 'undefined') return null;
  const v = localStorage.getItem(SAVE_KEY);
  return v ? Number(v) : null;
}

export function setSaveGameId(id: number) {
  localStorage.setItem(SAVE_KEY, String(id));
}

/** 익명 유저 보장: 없으면 생성 후 localStorage에 보관 */
export async function ensureUser(): Promise<string> {
  const existing = getUserId();
  if (existing) return existing;
  const user = await api<User>('POST', '/users');
  localStorage.setItem(USER_KEY, user.id);
  return user.id;
}
