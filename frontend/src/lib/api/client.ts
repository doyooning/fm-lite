import type { ApiEnvelope } from '@/types/api';

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

export async function api<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('fmlite.token') : null;
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  let envelope: ApiEnvelope<T> | null = null;
  try {
    envelope = (await res.json()) as ApiEnvelope<T>;
  } catch {
    /* JSON 아님 */
  }

  if (!res.ok || !envelope?.success) {
    // 401: 토큰 만료/무효 → 정리 후 로그인으로 유도 (auth 엔드포인트 자체 호출은 제외)
    if (res.status === 401 && typeof window !== 'undefined' && !path.startsWith('/auth/')) {
      localStorage.removeItem('fmlite.token');
      localStorage.removeItem('fmlite.saveGameId');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    throw new ApiError(
      envelope?.error?.code ?? 'UNKNOWN',
      envelope?.error?.message ?? `요청 실패 (${res.status})`,
      res.status,
    );
  }
  return envelope.data;
}
