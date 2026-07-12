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

  // 기본 저장(라인업 자동 = 베스트 XI)
  const def = await api('GET', `/matches/${m.matchId}/tactics/me`);
  assert(Array.isArray(def.tactic.lineup) && def.tactic.lineup.length === 11, 'default lineup should be 11, got ' + (def.tactic.lineup?.length));

  // 커스텀 라인업: 4-3-3 베스트 XI 에서 후보 MF 한 명을 교체 투입
  const players = await api('GET', `/teams/${m.isUserHome ? m.homeTeam.id : m.awayTeam.id}/players`);
  const bestXI = def.tactic.lineup;
  const benchMf = players.find(p => p.position === 'MF' && !bestXI.includes(p.id));
  const starterMf = players.filter(p => p.position === 'MF' && bestXI.includes(p.id))
    .sort((a, b) => a.overall - b.overall)[0];
  const customLineup = bestXI.map(id => id === starterMf.id ? benchMf.id : id);

  await api('PUT', `/matches/${m.matchId}/tactics/me`, {
    formation: '4-3-3', mentality: 'BALANCED', pressing: 'HIGH', lineHeight: 'NORMAL', attackStyle: 'WIDE',
    lineup: customLineup,
  });
  const tac = await api('GET', `/matches/${m.matchId}/tactics/me`);
  assert(tac.submitted === true && tac.tactic.pressing === 'HIGH', 'tactic roundtrip');
  assert(tac.tactic.lineup.includes(benchMf.id) && !tac.tactic.lineup.includes(starterMf.id), 'custom lineup persisted (swap)');
  console.log(`  전술 저장 OK (라인업 교체: ${starterMf.name} → ${benchMf.name})`);

  // 잘못된 라인업(10명)은 거부되어야 함
  const bad = await fetch(BASE + `/matches/${m.matchId}/tactics/me`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json', 'X-User-Id': userId },
    body: JSON.stringify({ formation: '4-3-3', mentality: 'BALANCED', pressing: 'HIGH', lineHeight: 'NORMAL', attackStyle: 'WIDE', lineup: customLineup.slice(0, 10) }),
  });
  assert(bad.status === 400, 'invalid lineup should 400, got ' + bad.status);

  let progress = await api('POST', `/matches/${m.matchId}/start`);
  let allEvents = [...progress.events];
  let choices = 0, halftimes = 0;
  while (progress.matchStatus === 'WAITING_CHOICE' || progress.matchStatus === 'WAITING_HALFTIME') {
    if (progress.matchStatus === 'WAITING_HALFTIME') {
      console.log(`  ⏸ 하프타임 전술 변경 (3-5-2 수비적, 라인업 자동)`);
      progress = await api('POST', `/matches/${m.matchId}/halftime-tactics`, {
        formation: '3-5-2', mentality: 'DEFENSIVE', pressing: 'NORMAL', lineHeight: 'LOW', attackStyle: 'COUNTER',
      });
      allEvents.push(...progress.events);
      halftimes++;
      continue;
    }
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
  assert(halftimes === 1, 'expected 1 halftime, got ' + halftimes);

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
