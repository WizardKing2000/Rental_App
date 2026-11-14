import test from 'node:test';
import assert from 'node:assert/strict';

const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
const databaseHost = process.env.FIREBASE_DATABASE_EMULATOR_HOST;
assert.ok(authHost && databaseHost, 'Run with Firebase emulators:exec; live services are never used.');
assert.match(authHost, /^(127\.0\.0\.1|localhost):\d+$/);
assert.match(databaseHost, /^(127\.0\.0\.1|localhost):\d+$/);

async function createUser(email) {
  const response = await fetch(
    `http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo-key`,
    {method: 'POST', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({email, password: 'Password10!', returnSecureToken: true})}
  );
  assert.equal(response.status, 200, 'Emulator signup must succeed');
  return response.json();
}

async function profileRequest(user, uid, method = 'GET', body) {
  const params = new URLSearchParams({ns: 'demo-yaadride-accounts-default-rtdb'});
  if (user) params.set('auth', user.idToken);
  return fetch(`http://${databaseHost}/accountProfiles/${uid}.json?${params}`, {
    method, headers: {'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}

test('account profiles enforce ownership, valid roles, and immutable account types', async (t) => {
  const owner = await createUser('owner@yaadride.test');
  const renter = await createUser('renter@yaadride.test');
  await t.test('users can create their own owner and renter profiles', async () => {
    assert.equal((await profileRequest(owner, owner.localId, 'PUT', {name: 'Owner', role: 'OWNER'})).status, 200);
    assert.equal((await profileRequest(renter, renter.localId, 'PUT', {name: 'Renter', role: 'RENTER'})).status, 200);
  });
  await t.test('users can read only their own profile', async () => {
    const response = await profileRequest(owner, owner.localId);
    assert.equal(response.status, 200);
    assert.equal((await response.json()).role, 'OWNER');
    assert.equal((await profileRequest(owner, renter.localId)).status, 401);
    assert.equal((await profileRequest(null, owner.localId)).status, 401);
  });
  await t.test('a renter cannot change their role or overwrite another profile', async () => {
    assert.equal((await profileRequest(renter, renter.localId, 'PUT', {name: 'Renter', role: 'OWNER'})).status, 401);
    assert.equal((await profileRequest(renter, owner.localId, 'PUT', {name: 'Renter', role: 'OWNER'})).status, 401);
    assert.equal((await profileRequest(owner, owner.localId, 'DELETE')).status, 401);
  });
  await t.test('invalid roles and extra profile fields are rejected', async () => {
    const fresh = await createUser('invalid-role@yaadride.test');
    assert.equal((await profileRequest(fresh, fresh.localId, 'PUT', {name: 'Tester', role: 'ADMIN'})).status, 401);
    assert.equal((await profileRequest(fresh, fresh.localId, 'PUT', {name: 'Tester', role: 'OWNER', admin: true})).status, 401);
    assert.equal((await profileRequest(fresh, fresh.localId, 'PUT', {name: '', role: 'OWNER'})).status, 401);
  });
});
