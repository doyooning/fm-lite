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
  const userId = typeof window !== 'undefined' ? localStorage.getItem('fmlite.userId') : null;
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(userId ? { 'X-User-Id': userId } : {}),
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
    throw new ApiError(
      envelope?.error?.code ?? 'UNKNOWN',
      envelope?.error?.message ?? `요청 실패 (${res.status})`,
      res.status,
    );
  }
  return envelope.data;
}
