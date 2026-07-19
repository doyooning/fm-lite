import { api } from '@/lib/api/client';
import type { User } from '@/types/api';

const TOKEN_KEY = 'fmlite.token';
const SAVE_KEY = 'fmlite.saveGameId';

export function getToken(): string | null {
  return typeof window === 'undefined' ? null : localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(SAVE_KEY);
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

/** 현재 로그인 사용자 조회 (토큰 없으면 null, 만료/무효면 토큰 정리) */
export async function fetchMe(): Promise<User | null> {
  if (!getToken()) return null;
  try {
    return await api<User>('GET', '/auth/me');
  } catch {
    clearToken();
    return null;
  }
}

export function logout() {
  clearToken();
}

export function getSaveGameId(): number | null {
  if (typeof window === 'undefined') return null;
  const v = localStorage.getItem(SAVE_KEY);
  return v ? Number(v) : null;
}

export function setSaveGameId(id: number) {
  localStorage.setItem(SAVE_KEY, String(id));
}
