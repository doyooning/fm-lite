// 인증 e2e: 회원가입 → (DB에서 토큰조회) 이메일인증 → 로그인 → 보호 API 접근
// + 엣지케이스: 미인증 로그인 403, 중복가입 409, 잘못된 비번 401, 무토큰 401
// 실행: SB_DB_PW=<pw> node tools/auth-e2e.mjs
import pkg from 'pg';
const { Client } = pkg;

const BASE = process.argv[2] ?? 'http://localhost:8080/api/v1';
const email = `e2e_${Date.now()}@example.com`;
const password = 'Passw0rd!';

async function call(method, path, { body, token } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json };
}
function assert(cond, msg) { if (!cond) throw new Error('ASSERT FAIL: ' + msg); }

// 1. 회원가입
let r = await call('POST', '/auth/register', { body: { email, password, nickname: 'E2E감독' } });
assert(r.status === 200 && r.json.success, 'register ok: ' + JSON.stringify(r.json));
console.log('✔ register:', email);

// 2. 중복가입 → 409
r = await call('POST', '/auth/register', { body: { email, password } });
assert(r.status === 409 && r.json.error?.code === 'EMAIL_TAKEN', 'duplicate register 409, got ' + r.status);
console.log('✔ duplicate email rejected (409 EMAIL_TAKEN)');

// 3. 미인증 로그인 → 403 EMAIL_NOT_VERIFIED
r = await call('POST', '/auth/login', { body: { email, password } });
assert(r.status === 403 && r.json.error?.code === 'EMAIL_NOT_VERIFIED', 'login before verify 403, got ' + r.status + ' ' + JSON.stringify(r.json));
console.log('✔ login before verification blocked (403 EMAIL_NOT_VERIFIED)');

// 4. DB에서 인증 토큰 조회
const c = new Client({ host: 'aws-0-ap-southeast-2.pooler.supabase.com', port: 5432, user: 'postgres.klkjooqskbmrskbjowrb', password: process.env.SB_DB_PW, database: 'postgres', ssl: { rejectUnauthorized: false } });
await c.connect();
const tok = await c.query(
  `select t.token from email_verification_tokens t
   join users u on u.id = t.user_id
   where lower(u.email) = lower($1) order by t.created_at desc limit 1`, [email]);
assert(tok.rows.length === 1, 'verification token row exists');
const token = tok.rows[0].token;
console.log('✔ verification token found in DB');

// 5. 잘못된 토큰 → 400
r = await call('POST', '/auth/verify', { body: { token: 'bogus-token-xyz' } });
assert(r.status === 400 && r.json.error?.code === 'INVALID_OR_EXPIRED_TOKEN', 'bad token 400, got ' + r.status);
console.log('✔ invalid token rejected (400)');

// 6. 이메일 인증
r = await call('POST', '/auth/verify', { body: { token } });
assert(r.status === 200 && r.json.success, 'verify ok: ' + JSON.stringify(r.json));
console.log('✔ email verified');

// 7. 잘못된 비밀번호 → 401
r = await call('POST', '/auth/login', { body: { email, password: 'wrongpass' } });
assert(r.status === 401 && r.json.error?.code === 'INVALID_CREDENTIALS', 'wrong pw 401, got ' + r.status);
console.log('✔ wrong password rejected (401)');

// 8. 정상 로그인 → JWT
r = await call('POST', '/auth/login', { body: { email, password } });
assert(r.status === 200 && r.json.data?.token, 'login returns token');
const jwt = r.json.data.token;
assert(r.json.data.user.email === email && r.json.data.user.emailVerified === true, 'user payload');
console.log('✔ login success, JWT issued');

// 9. 무토큰 보호 API → 401
r = await call('GET', '/teams');
assert(r.status === 401, 'no-token protected 401, got ' + r.status);
console.log('✔ protected API without token → 401');

// 10. Bearer 로 보호 API 접근
r = await call('GET', '/auth/me', { token: jwt });
assert(r.status === 200 && r.json.data.email === email, '/me with token');
r = await call('GET', '/teams', { token: jwt });
assert(r.status === 200 && r.json.data.length === 8, 'teams with token');
console.log('✔ authenticated access works (/me, /teams)');

// 11. Bearer 로 새 게임 생성 (익명 자동생성 없이 인증 사용자로)
r = await call('POST', '/save-games', { body: { teamId: r.json.data.find(t => t.grade === 'MID').id }, token: jwt });
assert(r.status === 200 && r.json.data?.id, 'create save game with auth');
const saveId = r.json.data.id;
console.log(`✔ save game created (#${saveId}) as authenticated user`);

// 12. 남의 저장게임 접근 차단: 다른 토큰으로 접근 → 403 (다른 계정 생성)
const email2 = `e2e2_${Date.now()}@example.com`;
await call('POST', '/auth/register', { body: { email: email2, password } });
const tok2 = await c.query(`select t.token from email_verification_tokens t join users u on u.id=t.user_id where lower(u.email)=lower($1) order by t.created_at desc limit 1`, [email2]);
await call('POST', '/auth/verify', { body: { token: tok2.rows[0].token } });
const jwt2 = (await call('POST', '/auth/login', { body: { email: email2, password } })).json.data.token;
r = await call('GET', `/save-games/${saveId}`, { token: jwt2 });
assert(r.status === 403, "other user's save game → 403, got " + r.status);
console.log('✔ ownership enforced (other user → 403)');

await c.end();
console.log('\n✅ AUTH E2E PASS');
