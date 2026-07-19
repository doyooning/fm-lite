// 백엔드 API 응답 타입 (docs/04-api-spec.md 와 1:1)

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
}

export interface User {
  id: string;
  email: string | null;
  nickname: string;
  emailVerified: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface TeamSummary {
  id: number;
  name: string;
  shortName: string;
  grade: 'STRONG' | 'UPPER_MID' | 'MID' | 'WEAK';
  gradeLabel: string;
  description: string;
  avgRating: number;
}

export interface PowerByArea {
  attack: number;
  midfield: number;
  defense: number;
  goalkeeping: number;
}

export interface TeamDetail extends TeamSummary {
  powerByArea: PowerByArea;
  keyPlayers: { id: number; name: string; position: string; overall: number }[];
}

export interface PlayerStats {
  attack: number;
  defense: number;
  passing: number;
  speed: number;
  stamina: number;
  mentality: number;
  finishing: number;
  goalkeeping: number;
}

export interface Trait {
  code: string;
  name: string;
  description: string;
  positive: boolean;
}

export interface Player {
  id: number;
  name: string;
  position: 'GK' | 'DF' | 'MF' | 'FW';
  backNumber: number;
  overall: number;
  stats: PlayerStats;
  traits: Trait[];
}

export interface TeamBrief {
  id: number;
  name: string;
  shortName: string;
}

export interface SaveGame {
  id: number;
  team: TeamBrief;
  teamGrade: string;
  managerName: string;
  status: 'IN_PROGRESS' | 'CHAMPION' | 'ELIMINATED';
  competitionId: number;
  currentRound: string;
  currentRoundLabel: string;
  winnerTeamId: number | null;
  createdAt: string;
}

export interface NextMatch {
  matchId: number;
  round: string;
  roundLabel: string;
  status: MatchStatus;
  homeTeam: TeamBrief;
  awayTeam: TeamBrief;
  isUserHome: boolean;
  tacticSubmitted: boolean;
}

export interface NextMatchResponse {
  saveGameStatus: SaveGame['status'];
  hasNext: boolean;
  match: NextMatch | null;
}

export type MatchStatus =
  | 'SCHEDULED' | 'IN_PROGRESS' | 'WAITING_CHOICE' | 'WAITING_HALFTIME' | 'FINISHED';

export type Formation = '4-3-3' | '4-2-3-1' | '3-5-2';

export interface Tactic {
  formation: Formation;
  mentality: 'ATTACKING' | 'BALANCED' | 'DEFENSIVE';
  pressing: 'LOW' | 'NORMAL' | 'HIGH';
  lineHeight: 'LOW' | 'NORMAL' | 'HIGH';
  attackStyle: 'CENTER' | 'WIDE' | 'COUNTER' | 'POSSESSION';
  lineup?: number[];
}

export interface TacticResponse {
  submitted: boolean;
  tactic: Tactic;
}

export interface ChoiceOption {
  id: string;
  label: string;
  description: string;
}

export interface MatchEvent {
  eventId: number;
  seq: number;
  minute: number;
  eventType:
    | 'KICK_OFF' | 'CHANCE' | 'GOAL' | 'SAVE' | 'MISS' | 'HALF_TIME'
    | 'TACTIC_CHANGE' | 'CHOICE' | 'COACH_DECISION' | 'FULL_TIME' | 'PENALTY_SHOOTOUT';
  teamId: number | null;
  playerId: number | null;
  description: string;
  requiresChoice: boolean;
  choiceOptions: ChoiceOption[] | null;
  selectedChoiceId: string | null;
}

export interface Score {
  home: number;
  away: number;
}

export interface MatchProgress {
  matchStatus: MatchStatus;
  score: Score;
  minute: number;
  events: MatchEvent[];
}

export interface MatchInfo {
  matchId: number;
  round: string;
  roundLabel: string;
  status: MatchStatus;
  homeTeam: TeamBrief;
  awayTeam: TeamBrief;
  isUserMatch: boolean;
  isUserHome: boolean;
  tacticSubmitted: boolean;
  score: Score | null;
}

export interface MatchStats {
  possessionHome: number;
  possessionAway: number;
  shotsHome: number;
  shotsAway: number;
  shotsOnTargetHome: number;
  shotsOnTargetAway: number;
  bestPlayerId: number;
  bestPlayerName: string;
}

export interface MatchResult {
  matchId: number;
  round: string;
  roundLabel: string;
  homeTeam: TeamBrief;
  awayTeam: TeamBrief;
  homeScore: number;
  awayScore: number;
  penaltyHomeScore: number | null;
  penaltyAwayScore: number | null;
  winnerTeamId: number;
  userWon: boolean | null;
  saveGameStatus: SaveGame['status'];
  stats: MatchStats;
}

export interface OpponentAnalysis {
  team: { id: number; name: string; shortName: string; grade: string; gradeLabel: string };
  powerByArea: PowerByArea;
  myPowerByArea: PowerByArea;
  strengths: string[];
  weaknesses: string[];
  keyPlayers: { id: number; name: string; position: string; overall: number; reason: string }[];
  expectedTactic: Tactic;
}

export interface BracketMatch {
  matchId: number;
  matchNo: number;
  homeTeam: TeamBrief;
  awayTeam: TeamBrief;
  status: MatchStatus;
  isUserMatch: boolean;
  homeScore: number | null;
  awayScore: number | null;
  penaltyHomeScore: number | null;
  penaltyAwayScore: number | null;
  winnerTeamId: number | null;
}

export interface Bracket {
  id: number;
  name: string;
  currentRound: string;
  currentRoundLabel: string;
  winnerTeamId: number | null;
  rounds: { round: string; roundLabel: string; matches: BracketMatch[] }[];
}
