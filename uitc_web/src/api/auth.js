import http from './http'

// POST /user/login {account,password} → {token,userId,username}
export function login(account, password) {
  return http.post('/user/login', { account, password })
}

export function register(account, password) {
  return http.post('/user/register', { account, password, username: account })
}
