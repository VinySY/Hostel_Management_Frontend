import test from 'node:test';
import assert from 'node:assert/strict';
import {
  WARDEN_PASSWORD_MAX_LENGTH,
  WARDEN_PASSWORD_MIN_LENGTH,
  buildCreateWardenPayload,
  submitCreateWardenRequest,
  validateWardenCreatePassword,
} from './wardenForm.ts';

test('validateWardenCreatePassword enforces backend min/max bounds without leaking values', () => {
  assert.equal(validateWardenCreatePassword(''), 'Password is required');
  assert.equal(validateWardenCreatePassword('     '), 'Password is required');

  const shortSecret = '12345';
  const shortErr = validateWardenCreatePassword(shortSecret);
  assert.match(shortErr, /between 6 and 100 characters/);
  assert.ok(!shortErr.includes(shortSecret));

  const longSecret = 'a'.repeat(WARDEN_PASSWORD_MAX_LENGTH + 1);
  const longErr = validateWardenCreatePassword(longSecret);
  assert.match(longErr, /between 6 and 100 characters/);
  assert.ok(!longErr.includes(longSecret));

  assert.equal(validateWardenCreatePassword('a'.repeat(WARDEN_PASSWORD_MIN_LENGTH)), null);
  assert.equal(validateWardenCreatePassword('a'.repeat(WARDEN_PASSWORD_MAX_LENGTH)), null);
});

test('buildCreateWardenPayload sends exact backend password field without trimming password', () => {
  const payload = buildCreateWardenPayload({
    employeeId: ' WAR-401 ',
    name: ' Dr. Warden ',
    email: ' warden@hostelhub.edu ',
    phone: ' +91 98765 43210 ',
    hostelId: 3,
    password: 'Warden!Pass#2026',
    assignedFloors: [1, 2],
  });

  assert.deepEqual(payload, {
    employeeId: 'WAR-401',
    name: 'Dr. Warden',
    email: 'warden@hostelhub.edu',
    phone: '+91 98765 43210',
    hostelId: 3,
    password: 'Warden!Pass#2026',
    assignedFloors: [1, 2],
  });
});

test('submitCreateWardenRequest posts password in JSON body and throws backend HTTP 400 error message', async () => {
  let capturedUrl = '';
  let capturedInit = null;
  const mockSuccessFetch = async (url, init) => {
    capturedUrl = url;
    capturedInit = init;
    return {
      ok: true,
      json: async () => ({ id: 99, employeeId: 'WAR-401', name: 'Dr. Warden', email: 'warden@hostelhub.edu', phone: '+91 98765 43210', active: true, assignedFloors: [1] }),
    };
  };

  const payload = buildCreateWardenPayload({
    employeeId: 'WAR-401',
    name: 'Dr. Warden',
    email: 'warden@hostelhub.edu',
    phone: '+91 98765 43210',
    hostelId: 2,
    password: 'Warden!Pass#2026',
    assignedFloors: [1],
  });

  const created = await submitCreateWardenRequest(
    mockSuccessFetch,
    'https://api.example.com/api',
    { 'Content-Type': 'application/json', Authorization: 'Bearer token' },
    payload
  );

  assert.equal(capturedUrl, 'https://api.example.com/api/dgm/wardens');
  assert.equal(capturedInit.method, 'POST');
  assert.equal(JSON.parse(capturedInit.body).password, 'Warden!Pass#2026');
  assert.equal(created.password, undefined);

  const mockErrorFetch = async () => ({
    ok: false,
    status: 400,
    json: async () => ({ message: 'Password is required' }),
  });

  await assert.rejects(
    () => submitCreateWardenRequest(mockErrorFetch, 'https://api.example.com/api', {}, payload),
    { message: 'Password is required' }
  );
});

