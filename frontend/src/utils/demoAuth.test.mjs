import test from 'node:test';
import assert from 'node:assert/strict';
import {
  extractErrorMessageFromBody,
  isPublicDemoLoginEnabled,
  submitDemoLoginRequest,
} from './demoAuth.ts';

test('isPublicDemoLoginEnabled only enables for explicit true values', () => {
  assert.equal(isPublicDemoLoginEnabled('true'), true);
  assert.equal(isPublicDemoLoginEnabled(true), true);
  assert.equal(isPublicDemoLoginEnabled('false'), false);
  assert.equal(isPublicDemoLoginEnabled(false), false);
  assert.equal(isPublicDemoLoginEnabled(undefined), false);
  assert.equal(isPublicDemoLoginEnabled(''), false);
});

test('submitDemoLoginRequest posts passwordless role payload to /auth/demo-login', async () => {
  let capturedUrl = '';
  let capturedInit = null;

  const mockFetch = async (url, init) => {
    capturedUrl = url;
    capturedInit = init;
    return {
      ok: true,
      status: 200,
      json: async () => ({
        token: 'jwt-demo-token',
        user: {
          id: '101',
          name: 'Aarav Kulkarni (Demo)',
          email: 'demo.student@demo.hostelsync.local',
          role: 'STUDENT',
          demo: true,
        },
      }),
    };
  };

  const result = await submitDemoLoginRequest(mockFetch, 'https://api.example.com/api', 'STUDENT');

  assert.equal(capturedUrl, 'https://api.example.com/api/auth/demo-login');
  assert.equal(capturedInit.method, 'POST');
  const parsedBody = JSON.parse(capturedInit.body);
  assert.deepEqual(parsedBody, { role: 'STUDENT' });
  assert.equal(parsedBody.password, undefined);
  assert.equal(result.user.demo, true);
});

test('submitDemoLoginRequest surfaces backend 403 error when public demo is disabled', async () => {
  const mockDisabledFetch = async () => ({
    ok: false,
    status: 403,
    json: async () => ({ message: 'Public Demo Mode is currently disabled on this server.' }),
  });

  await assert.rejects(
    () => submitDemoLoginRequest(mockDisabledFetch, 'https://api.example.com/api', 'DGM'),
    { message: 'Public Demo Mode is currently disabled on this server.' }
  );
});

test('extractErrorMessageFromBody surfaces backend 403 read-only demo message', () => {
  const msg = extractErrorMessageFromBody(
    { message: 'Demo accounts are read-only. Modifying, creating, or deleting records is disabled in Demo Mode.' },
    403,
    'Fallback failure'
  );
  assert.match(msg, /read-only/i);

  const fallback403 = extractErrorMessageFromBody(null, 403, 'Fallback failure');
  assert.match(fallback403, /read-only/i);
});

