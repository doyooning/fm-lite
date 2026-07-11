// FM Lite E2E 스모크 테스트: 새 게임 → 전술 → 경기(선택지 응답) → 토너먼트 완주
// 실행: node tools/e2e-test.mjs [API_BASE]
const BASE = process.argv[2] ?? 'http://localhost:8080/api/v1';

let userId = null;

async function api(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(userId ? { 'X-User-Id': userId } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || json.success === false) {
    throw new Error(`${method} ${path} -> ${res.status} ${JSON.stringify(json.error ?? json)}`);
  }
  return json.data;
}

function assert(cond, msg) {
  if (!cond) throw new Error('ASSERT FAIL: ' + msg);
}

const user = await api('POST', '/users', { nickname: 'E2E감독' });
userId = user.id;
console.log('✔ user created:', userId);

const teams = await api('GET', '/teams');
assert(teams.length === 8, 'teams=8, got ' + teams.length);
const myTeam = teams.find(t => t.grade === 'MID'); // 중위팀으로 도전
console.log('✔ teams listed. picking:', myTeam.name);

const detail = await api('GET', `/teams/${myTeam.id}`);
assert(detail.powerByArea.midfield > 0, 'team detail powerByArea');
const players = await api('GET', `/teams/${myTeam.id}/players`);
assert(players.length === 18, 'players=18, got ' + players.length);
const byPos = players.reduce((a, p) => ((a[p.position] = (a[p.position] ?? 0) + 1), a), {});
assert(byPos.GK === 2 && byPos.DF === 6 && byPos.MF === 6 && byPos.FW === 4, 'squad plan ' + JSON.stringify(byPos));
console.log('✔ team detail + squad OK', JSON.stringify(byPos));

const save = await api('POST', '/save-games', { teamId: myTeam.id });
console.log(`✔ save game #${save.id}, competition #${save.competitionId}, round=${save.currentRound}`);

let matchCount = 0;
let championReached = false;

while (true) {
  const next = await api('GET', `/save-games/${save.id}/next-match`);
  if (!next.hasNext) {
    console.log(`— no next match. saveGameStatus=${next.saveGameStatus}`);
    championReached = next.saveGameStatus === 'CHAMPION';
    break;
  }
  const m = next.match;
  matchCount++;
  console.log(`\n=== ${m.roundLabel} : ${m.homeTeam.name} vs ${m.awayTeam.name} (match #${m.matchId}) ===`);

  // 전술 설정 전 start 시도 → 409 기대
  const early = await fetch(BASE + `/matches/${m.matchId}/start`, { method: 'POST', headers: { 'X-User-Id': userId } });
  assert(early.status === 409, 'start before tactic should 409, got ' + early.status);

  const analysis = await api('GET', `/matches/${m.matchId}/opponent-analysis`);
  console.log(`  분석: 강점 ${analysis.strengths.length} / 약점 ${analysis.weaknesses.length} / 키플레이어 ${analysis.keyPlayers.length} / 예상전술 ${analysis.expectedTactic.formation}`);

  await api('PUT', `/matches/${m.matchId}/tactics/me`, {
    formation: '4-3-3', mentality: 'BALANCED', pressing: 'HIGH', lineHeight: 'NORMAL', attackStyle: 'WIDE',
  });
  const tac = await api('GET', `/matches/${m.matchId}/tactics/me`);
  assert(tac.submitted === true && tac.tactic.pressing === 'HIGH', 'tactic roundtrip');
  console.log('  전술 저장 OK');

  let progress = await api('POST', `/matches/${m.matchId}/start`);
  let allEvents = [...progress.events];
  let choices = 0;
  while (progress.matchStatus === 'WAITING_CHOICE') {
    const choiceEvent = progress.events.filter(e => e.requiresChoice).pop();
    assert(choiceEvent, 'WAITING_CHOICE but no choice event');
    const pick = choiceEvent.choiceOptions[choices % choiceEvent.choiceOptions.length];
    console.log(`  ${choiceEvent.minute}' 선택지: "${choiceEvent.description}" -> [${pick.label}]`);
    progress = await api('POST', `/matches/${m.matchId}/choices`, { eventId: choiceEvent.eventId, choiceId: pick.id });
    allEvents.push(...progress.events);
    choices++;
  }
  assert(progress.matchStatus === 'FINISHED', 'match should finish, got ' + progress.matchStatus);
  assert(choices === 3, 'expected 3 choices, got ' + choices);

  const result = await api('GET', `/matches/${m.matchId}/result`);
  const pen = result.penaltyHomeScore != null ? ` (PK ${result.penaltyHomeScore}:${result.penaltyAwayScore})` : '';
  console.log(`  결과: ${result.homeScore} : ${result.awayScore}${pen} → ${result.userWon ? '승리' : '패배'} | MOM ${result.stats.bestPlayerName} | 점유 ${result.stats.possessionHome}:${result.stats.possessionAway} | 슛 ${result.stats.shotsHome}:${result.stats.shotsAway}`);
  console.log(`  이벤트 ${allEvents.length}건 (골 ${allEvents.filter(e => e.eventType === 'GOAL').length}, 전술변화 ${allEvents.filter(e => e.eventType === 'TACTIC_CHANGE').length})`);

  // 이벤트 복구 API
  const replay = await api('GET', `/matches/${m.matchId}/events`);
  assert(replay.events.length >= allEvents.length, 'events replay');

  if (!result.userWon) {
    const after = await api('GET', `/save-games/${save.id}`);
    assert(after.status === 'ELIMINATED', 'should be eliminated, got ' + after.status);
    console.log('\n— 탈락. 대회는 자동 완주되어야 함.');
    break;
  }
}

const bracket = await api('GET', `/competitions/${save.competitionId}`);
console.log(`\n=== 대진표 (currentRound=${bracket.currentRound}, winner=${bracket.winnerTeamId}) ===`);
for (const r of bracket.rounds) {
  for (const bm of r.matches) {
    const score = bm.homeScore != null ? `${bm.homeScore}:${bm.awayScore}` : '-';
    const pk = bm.penaltyHomeScore != null ? `(PK ${bm.penaltyHomeScore}:${bm.penaltyAwayScore})` : '';
    console.log(`  [${r.roundLabel}] ${bm.homeTeam.shortName} ${score}${pk} ${bm.awayTeam.shortName} ${bm.isUserMatch ? '★' : ''} ${bm.status}`);
  }
}
assert(bracket.currentRound === 'FINISHED', 'competition must be finished, got ' + bracket.currentRound);
assert(bracket.rounds.every(r => r.matches.every(mm => mm.status === 'FINISHED')), 'all matches finished');
assert(bracket.winnerTeamId != null, 'winner set');

const finalSave = await api('GET', `/save-games/${save.id}`);
console.log(`\n최종: 사용자 ${matchCount}경기 진행, saveGame=${finalSave.status}, 우승팀 ID=${bracket.winnerTeamId}`);
assert(['CHAMPION', 'ELIMINATED'].includes(finalSave.status), 'final status');

const list = await api('GET', `/users/${userId}/save-games`);
assert(list.length === 1, 'save game list');

console.log('\n✅ E2E PASS');
