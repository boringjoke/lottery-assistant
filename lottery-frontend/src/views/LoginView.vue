<script setup lang="ts">
import starIconUrl from '@/assets/icons/star.svg'
import tipIconUrl from '@/assets/icons/tip.svg'
import { loginWithPassword } from '@/api/auth'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getErrorMessage } from '@/utils/lotteryFormat'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const returnTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/lottery-assistant?tab=overview'
})

/**
 * 调用账号密码登录接口；Cookie 由后端写入，前端只负责跳转和错误展示。
 */
async function submitLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '账号和密码不能为空'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await loginWithPassword({
      account: username.value.trim(),
      password: password.value,
    })
    await router.push(returnTarget.value)
  } catch (err) {
    errorMessage.value = getErrorMessage(err, '登录失败，请检查账号和密码')
  } finally {
    submitting.value = false
  }
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
            <div class="input-field">
              <input
                id="username"
                v-model="username"
                autocomplete="username"
                placeholder="请输入用户名"
                type="text"
              />
              <button
                v-if="username"
                class="input-clear-button"
                data-testid="clear-username"
                type="button"
                aria-label="清除用户名"
                @click="username = ''"
              >
                <span aria-hidden="true"></span>
              </button>
            </div>
          </div>

          <div class="form-field">
            <label for="password">密码</label>
            <div class="input-field password-field">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
              <button
                v-if="password"
                class="input-clear-button input-clear-button--password"
                data-testid="clear-password"
                type="button"
                aria-label="清除密码"
                @click="password = ''"
              >
                <span aria-hidden="true"></span>
              </button>
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

          <div v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</div>

          <button class="login-submit" type="submit" :disabled="submitting">
            {{ submitting ? '登录中' : '登录' }}
          </button>

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
          <img class="login-note__svg" :src="tipIconUrl" alt="" />
        </span>
        <span>游客无需登录也可以使用开奖查询和号码分析功能</span>
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

.input-field {
  position: relative;
}

.input-field input {
  padding-right: 46px;
}

.form-field input:focus {
  border-color: #2563eb;
  outline: 3px solid #bfdbfe;
}

.password-field input {
  padding-right: 84px;
}

.input-clear-button {
  position: absolute;
  top: 50%;
  right: 8px;
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transform: translateY(-50%);
}

.input-clear-button:hover {
  background: #f1f5f9;
  color: #475569;
}

.input-clear-button--password {
  right: 42px;
}

.input-clear-button span {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.78;
}

.input-clear-button span::before,
.input-clear-button span::after {
  content: "";
  position: absolute;
  top: 6px;
  left: 3px;
  width: 8px;
  height: 2px;
  border-radius: 999px;
  background: #ffffff;
}

.input-clear-button span::before {
  transform: rotate(45deg);
}

.input-clear-button span::after {
  transform: rotate(-45deg);
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

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.login-error {
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
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
.input-clear-button:focus-visible,
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
