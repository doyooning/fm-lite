import { api } from '@/lib/api/client';
import type {
  Bracket, MatchInfo, MatchProgress, MatchResult, NextMatchResponse,
  OpponentAnalysis, Player, SaveGame, Tactic, TacticResponse, TeamDetail, TeamSummary,
} from '@/types/api';

export const teamsApi = {
  list: () => api<TeamSummary[]>('GET', '/teams'),
  detail: (teamId: number) => api<TeamDetail>('GET', `/teams/${teamId}`),
  players: (teamId: number) => api<Player[]>('GET', `/teams/${teamId}/players`),
};

export const saveGamesApi = {
  create: (teamId: number) => api<SaveGame>('POST', '/save-games', { teamId }),
  get: (id: number) => api<SaveGame>('GET', `/save-games/${id}`),
  nextMatch: (id: number) => api<NextMatchResponse>('GET', `/save-games/${id}/next-match`),
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
  events: (matchId: number) => api<MatchProgress>('GET', `/matches/${matchId}/events`),
  result: (matchId: number) => api<MatchResult>('GET', `/matches/${matchId}/result`),
};

export const competitionsApi = {
  bracket: (competitionId: number) => api<Bracket>('GET', `/competitions/${competitionId}`),
};
