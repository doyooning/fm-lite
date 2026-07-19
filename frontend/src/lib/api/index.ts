import { api } from '@/lib/api/client';
import type {
  AuthResponse, Bracket, MatchInfo, MatchProgress, MatchResult, NextMatchResponse,
  OpponentAnalysis, Player, SaveGame, Tactic, TacticResponse, TeamDetail, TeamSummary, User,
} from '@/types/api';

export const authApi = {
  register: (email: string, password: string, nickname?: string) =>
    api<{ message: string }>('POST', '/auth/register', { email, password, nickname }),
  login: (email: string, password: string) =>
    api<AuthResponse>('POST', '/auth/login', { email, password }),
  verify: (token: string) => api<{ message: string }>('POST', '/auth/verify', { token }),
  resend: (email: string) => api<{ message: string }>('POST', '/auth/resend-verification', { email }),
  me: () => api<User>('GET', '/auth/me'),
};

export const teamsApi = {
  list: () => api<TeamSummary[]>('GET', '/teams'),
  detail: (teamId: number) => api<TeamDetail>('GET', `/teams/${teamId}`),
  players: (teamId: number) => api<Player[]>('GET', `/teams/${teamId}/players`),
};

export const saveGamesApi = {
  create: (teamId: number) => api<SaveGame>('POST', '/save-games', { teamId }),
  get: (id: number) => api<SaveGame>('GET', `/save-games/${id}`),
  nextMatch: (id: number) => api<NextMatchResponse>('GET', `/save-games/${id}/next-match`),
  listMine: () => api<SaveGame[]>('GET', '/save-games'),
};

export const matchesApi = {
  info: (matchId: number) => api<MatchInfo>('GET', `/matches/${matchId}`),
  opponentAnalysis: (matchId: number) => api<OpponentAnalysis>('GET', `/matches/${matchId}/opponent-analysis`),
  getMyTactic: (matchId: number) => api<TacticResponse>('GET', `/matches/${matchId}/tactics/me`),
  saveMyTactic: (matchId: number, tactic: Tactic) =>
    api<TacticResponse>('PUT', `/matches/${matchId}/tactics/me`, tactic),
  start: (matchId: number) => api<MatchProgress>('POST', `/matches/${matchId}/start`),
  choose: (matchId: number, eventId: number, choiceId: string) =>
    api<MatchProgress>('POST', `/matches/${matchId}/choices`, { eventId, choiceId }),
  halftimeTactics: (matchId: number, tactic: Tactic) =>
    api<MatchProgress>('POST', `/matches/${matchId}/halftime-tactics`, tactic),
  events: (matchId: number) => api<MatchProgress>('GET', `/matches/${matchId}/events`),
  result: (matchId: number) => api<MatchResult>('GET', `/matches/${matchId}/result`),
};

export const competitionsApi = {
  bracket: (competitionId: number) => api<Bracket>('GET', `/competitions/${competitionId}`),
};
