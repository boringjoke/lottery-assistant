<script setup lang="ts">
import starIconUrl from '@/assets/icons/star.svg'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)

const returnTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/lottery-assistant?tab=overview'
})

/**
 * 当前只实现静态登录页，后续接入认证接口后再提交账号密码。
 */
function submitLogin() {
  void router.push(returnTarget.value)
}
</script>

<template>
  <div class="login-page">
    <header class="login-topbar">
      <button class="back-button" type="button" @click="router.push('/lottery-assistant?tab=overview')">
        <span class="back-button__icon" aria-hidden="true"></span>
        返回彩票助手
      </button>
    </header>

    <main class="login-main">
      <section class="login-card" aria-labelledby="loginTitle">
        <div class="login-card__glow"></div>

        <div class="login-brand">
          <div class="login-brand__mark">≋</div>
          <span>彩票助手</span>
        </div>

        <div class="login-heading">
          <h1 id="loginTitle">欢迎回来</h1>
          <p>请输入账号和密码完成登录</p>
        </div>

        <form class="login-form" @submit.prevent="submitLogin">
          <div class="form-field">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model="username"
              autocomplete="username"
              placeholder="请输入用户名"
              type="text"
            />
          </div>

          <div class="form-field">
            <label for="password">密码</label>
            <div class="password-field">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
              <button
                class="password-toggle"
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <span class="password-toggle__icon" :class="{ visible: showPassword }" aria-hidden="true"></span>
              </button>
            </div>
          </div>

          <button class="login-submit" type="submit">登录</button>

          <div class="login-links">
            <button type="button">注册账号</button>
            <button type="button">忘记密码？</button>
          </div>
        </form>
      </section>

      <p class="login-note login-note--primary">
        <span class="login-note__icon" aria-hidden="true">
          <img class="login-note__svg" :src="starIconUrl" alt="" />
        </span>
        <span>登录后可收藏号码并查看开奖历史</span>
      </p>

      <p class="login-note">
        <span class="login-note__icon" aria-hidden="true">
          <svg class="login-note__svg" viewBox="0 0 1024 1024">
            <path
              d="M692.38 325.4a250.17 250.17 0 0 0-178.06-73.75h-4.64a251.8 251.8 0 0 0-251.81 251.81 325.89 325.89 0 0 0 30.62 138.68c17.45 37.45 41.85 72 72.53 102.66a384.23 384.23 0 0 0 37.71 33.08h226.54A384.23 384.23 0 0 0 663 744.8c30.68-30.67 55.08-65.21 72.53-102.66a325.89 325.89 0 0 0 30.62-138.68 250.17 250.17 0 0 0-73.77-178.06zM512 681.08a39.25 39.25 0 1 1 39.25-39.25A39.25 39.25 0 0 1 512 681.08z m40.5-230.62c-3.7 61.45-27.66 100.54-40.35 99.2-17.48-1.83-35.23-40.28-40.36-99.2-4.74-54.59 0.08-99.21 40.36-99.21 42.95 0 43.65 44.51 40.35 99.21z"
              fill="#F9DB91"
            />
            <path
              d="M394.92 777.88v54.37A115.13 115.13 0 0 0 509.67 947h4.66a115.13 115.13 0 0 0 114.75-114.75v-54.37H394.92z"
              fill="#EF6A6A"
            />
            <path
              d="M512.15 351.25c-40.28 0-45.1 44.62-40.36 99.21 5.13 58.92 22.88 97.37 40.36 99.2 12.69 1.34 36.65-37.75 40.35-99.2 3.3-54.7 2.6-99.21-40.35-99.21z"
              fill="#AEF0FF"
            />
            <path d="M512 641.83m-39.25 0a39.25 39.25 0 1 0 78.5 0 39.25 39.25 0 1 0-78.5 0Z" fill="#AEF0FF" />
            <path
              d="M514.32 238.65h-4.64c-146.25 0-264.81 118.56-264.81 264.81 0 126.57 67.48 221.06 137.05 277.67v51.12c0 70.26 57.49 127.75 127.75 127.75h4.66c70.26 0 127.75-57.49 127.75-127.75v-51.12c69.57-56.61 137-151.1 137-277.67 0.05-146.25-118.51-264.81-264.76-264.81z m101.76 593.6A102.12 102.12 0 0 1 514.33 934h-4.66a102.12 102.12 0 0 1-101.75-101.75v-41.37h208.16z m137-328.79a313 313 0 0 1-29.4 133.19c-16.81 36.07-40.34 69.36-69.94 99a369.71 369.71 0 0 1-33 29.28H403.21a371.15 371.15 0 0 1-33-29.28c-29.6-29.59-53.13-62.88-69.94-99a313 313 0 0 1-29.4-133.19 238.57 238.57 0 0 1 238.81-238.81h4.64a238.57 238.57 0 0 1 238.81 238.81z"
              fill="#512C56"
            />
            <path
              d="M455 863.94h114a13 13 0 0 0 0-26H455a13 13 0 0 0 0 26zM512 167.56a13 13 0 0 0 13-13V77a13 13 0 0 0-26 0v77.56a13 13 0 0 0 13 13zM282 262.84a13 13 0 0 0 9.19-22.2l-54.86-54.84A13 13 0 1 0 218 204.18L272.79 259a13 13 0 0 0 9.21 3.84zM186.71 466.85h-77.56a13 13 0 1 0 0 26h77.56a13 13 0 1 0 0-26zM914.85 466.85h-77.56a13 13 0 0 0 0 26h77.56a13 13 0 0 0 0-26zM787.66 185.8l-54.84 54.84A13 13 0 1 0 751.2 259l54.85-54.85a13 13 0 1 0-18.39-18.38zM512.15 338.25c-15.2 0-27.57 5.08-36.77 15.12-15.84 17.28-20.94 47.57-16.54 98.21 2.57 29.51 8.28 55.42 16.52 74.94 11.81 28 25.78 35.06 35.43 36.07a17.14 17.14 0 0 0 2 0.11c4.14 0 9.82-1.34 16.07-6.64 15.91-13.48 33.42-51.12 36.66-104.82 2.43-40.25 3-78.07-16.19-98.47-9.11-9.64-21.61-14.52-37.18-14.52z m27.38 111.42c-1.79 29.66-8.22 50.7-13.3 63.13-5.85 14.3-11.41 21-14.09 23.35-6.21-4-22.27-27.83-27.4-86.82-1.33-15.29-5.37-61.84 9.81-78.39 2.63-2.88 7.32-6.69 17.6-6.69 11 0 15.65 3.62 18.21 6.34 12.09 12.84 11.07 47.55 9.17 79.08zM512 589.58a52.25 52.25 0 1 0 52.25 52.25A52.31 52.31 0 0 0 512 589.58z m0 78.5a26.25 26.25 0 1 1 26.25-26.25A26.29 26.29 0 0 1 512 668.08z"
              fill="#512C56"
            />
          </svg>
        </span>
        <span>游客无需登录也可以使用开奖查询和号码分析功能。</span>
      </p>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgb(239 246 255 / 0.94), rgb(248 250 252 / 0.98)),
    radial-gradient(circle at 78% 18%, rgb(37 99 235 / 0.12), transparent 34%);
  color: #0f172a;
}

.login-topbar {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 10;
  width: 100%;
  padding: 24px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.back-button:hover {
  color: #0f172a;
}

.back-button__icon {
  width: 9px;
  height: 9px;
  border-bottom: 2px solid currentColor;
  border-left: 2px solid currentColor;
  transform: rotate(45deg);
}

.login-main {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 88px 20px 40px;
}

.login-card {
  position: relative;
  width: min(100%, 430px);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #ffffff;
  padding: 38px;
  box-shadow: 0 24px 70px rgb(15 23 42 / 0.11);
}

.login-card__glow {
  position: absolute;
  top: 0;
  right: 0;
  width: 140px;
  height: 140px;
  border-bottom-left-radius: 999px;
  background: linear-gradient(225deg, #dbeafe, transparent 72%);
  pointer-events: none;
}

.login-brand,
.login-heading,
.login-form,
.login-note {
  position: relative;
  z-index: 1;
}

.login-note--primary {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.login-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 26px;
  color: #1d4ed8;
  font-size: 21px;
  font-weight: 900;
}

.login-brand__mark {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  box-shadow: 0 8px 20px rgb(37 99 235 / 0.22);
}

.login-heading {
  margin-bottom: 28px;
  text-align: center;
}

.login-heading h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 900;
}

.login-heading p,
.login-note p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.login-form {
  display: grid;
  gap: 18px;
}

.form-field {
  display: grid;
  gap: 8px;
}

.form-field label {
  color: #334155;
  font-size: 14px;
  font-weight: 800;
}

.form-field input {
  width: 100%;
  height: 44px;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  color: #0f172a;
  padding: 0 14px;
  font-size: 14px;
}

.form-field input:focus {
  border-color: #2563eb;
  outline: 3px solid #bfdbfe;
}

.password-field {
  position: relative;
}

.password-field input {
  padding-right: 46px;
}

.password-toggle {
  position: absolute;
  top: 50%;
  right: 8px;
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transform: translateY(-50%);
}

.password-toggle:hover {
  background: #f1f5f9;
  color: #475569;
}

.password-toggle__icon {
  position: relative;
  width: 18px;
  height: 12px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.password-toggle__icon::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.password-toggle__icon::after {
  content: "";
  position: absolute;
  top: 50%;
  left: -3px;
  width: 24px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  transform: rotate(-35deg);
}

.password-toggle__icon.visible::after {
  display: none;
}

.login-submit {
  height: 44px;
  border: 0;
  border-radius: 12px;
  background: #2563eb;
  color: #ffffff;
  font-size: 16px;
  font-weight: 900;
  box-shadow: 0 12px 28px rgb(37 99 235 / 0.22);
  cursor: pointer;
}

.login-submit:hover {
  background: #1d4ed8;
}

.login-links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -2px;
}

.login-links button {
  border: 0;
  background: transparent;
  color: #2563eb;
  padding: 0;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.login-links button:last-child {
  color: #94a3b8;
}

.login-links button:hover {
  color: #1d4ed8;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.login-submit:focus-visible,
.back-button:focus-visible,
.password-toggle:focus-visible,
.login-links button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.login-note {
  display: flex;
  width: fit-content;
  max-width: min(100%, 430px);
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin: 18px 0 0;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: rgb(255 255 255 / 0.88);
  color: #64748b;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 12px 32px rgb(15 23 42 / 0.08);
  text-align: center;
  white-space: normal;
}

.login-note__icon {
  display: inline-flex;
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
}

.login-note__svg {
  display: block;
  width: 18px;
  height: 18px;
  object-fit: contain;
}

@media (max-width: 560px) {
  .login-card {
    padding: 30px 22px;
  }

  .login-topbar {
    padding: 18px;
  }
}
</style>
