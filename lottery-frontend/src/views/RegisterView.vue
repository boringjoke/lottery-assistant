<script setup lang="ts">
import starIconUrl from '@/assets/icons/star.svg'
import { registerAccount } from '@/api/auth'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getErrorMessage } from '@/utils/lotteryFormat'

const route = useRoute()
const router = useRouter()
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const returnTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/?tab=overview'
})
const passwordMismatch = computed(() => {
  return Boolean(confirmPassword.value) && password.value !== confirmPassword.value
})

function goLogin() {
  void router.push({
    path: '/login',
    query: { redirect: returnTarget.value },
  })
}

/**
 * 调用注册接口；注册成功后 Cookie 由后端写入，前端跳回目标页面。
 */
async function submitRegister() {
  const trimmedUsername = username.value.trim()
  const trimmedNickname = nickname.value.trim()

  if (!trimmedUsername || !trimmedNickname || !password.value || !confirmPassword.value) {
    errorMessage.value = '用户名、昵称和密码不能为空'
    return
  }
  if (!/^[A-Za-z0-9_]{4,32}$/.test(trimmedUsername)) {
    errorMessage.value = '用户名需为 4-32 位字母、数字或下划线'
    return
  }
  if (trimmedNickname.length > 64) {
    errorMessage.value = '昵称不能超过 64 个字符'
    return
  }
  if (password.value.length < 8 || password.value.length > 64) {
    errorMessage.value = '密码长度需为 8-64 位'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await registerAccount({
      username: trimmedUsername,
      nickname: trimmedNickname,
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    await router.push(returnTarget.value)
  } catch (err) {
    errorMessage.value = getErrorMessage(err, '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <header class="register-topbar">
      <button class="back-button" type="button" @click="router.push('/?tab=overview')">
        <span class="back-button__icon" aria-hidden="true"></span>
        返回彩票助手
      </button>
    </header>

    <main class="register-main">
      <section class="register-card" aria-labelledby="registerTitle">
        <div class="register-card__glow"></div>

        <div class="register-brand">
          <div class="register-brand__mark">≋</div>
          <span>彩票助手</span>
        </div>

        <div class="register-heading">
          <h1 id="registerTitle">创建账号</h1>
          <p>注册后可收藏号码并追踪中奖历史</p>
        </div>

        <form class="register-form" @submit.prevent="submitRegister">
          <div class="form-field">
            <label for="registerUsername">用户名</label>
            <div class="input-field">
              <input
                id="registerUsername"
                v-model="username"
                autocomplete="username"
                maxlength="32"
                placeholder="4-32 位字母、数字或下划线"
                type="text"
              />
              <button
                v-if="username"
                class="input-clear-button"
                type="button"
                aria-label="清除用户名"
                @click="username = ''"
              >
                <span aria-hidden="true"></span>
              </button>
            </div>
          </div>

          <div class="form-field">
            <label for="registerNickname">昵称</label>
            <div class="input-field">
              <input
                id="registerNickname"
                v-model="nickname"
                autocomplete="nickname"
                maxlength="64"
                placeholder="请输入昵称"
                type="text"
              />
              <button
                v-if="nickname"
                class="input-clear-button"
                type="button"
                aria-label="清除昵称"
                @click="nickname = ''"
              >
                <span aria-hidden="true"></span>
              </button>
            </div>
          </div>

          <div class="form-field">
            <label for="registerPassword">密码</label>
            <div class="input-field password-field">
              <input
                id="registerPassword"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="8-64 位密码"
              />
              <button
                v-if="password"
                class="input-clear-button input-clear-button--password"
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

          <div class="form-field">
            <label for="registerConfirmPassword">确认密码</label>
            <div class="input-field password-field">
              <input
                id="registerConfirmPassword"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="请再次输入密码"
              />
              <button
                v-if="confirmPassword"
                class="input-clear-button input-clear-button--password"
                type="button"
                aria-label="清除确认密码"
                @click="confirmPassword = ''"
              >
                <span aria-hidden="true"></span>
              </button>
              <button
                class="password-toggle"
                type="button"
                :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <span class="password-toggle__icon" :class="{ visible: showConfirmPassword }" aria-hidden="true"></span>
              </button>
            </div>
            <p v-if="passwordMismatch" class="form-field__hint form-field__hint--error">两次输入的密码不一致</p>
          </div>

          <div v-if="errorMessage" class="register-error" role="alert">{{ errorMessage }}</div>

          <button class="register-submit" type="submit" :disabled="submitting">
            {{ submitting ? '注册中' : '注册并登录' }}
          </button>

          <div class="register-links">
            <button type="button" @click="goLogin">已有账号？去登录</button>
          </div>
        </form>
      </section>

      <p class="register-note">
        <span class="register-note__icon" aria-hidden="true">
          <img class="register-note__svg" :src="starIconUrl" alt="" />
        </span>
        <span>昵称之后可在个人中心修改</span>
      </p>
    </main>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgb(239 246 255 / 0.94), rgb(248 250 252 / 0.98)),
    radial-gradient(circle at 78% 18%, rgb(37 99 235 / 0.12), transparent 34%);
  color: #0f172a;
}

.register-topbar {
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

.register-main {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 88px 20px 40px;
}

.register-card {
  position: relative;
  width: min(100%, 430px);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #ffffff;
  padding: 34px 38px;
  box-shadow: 0 24px 70px rgb(15 23 42 / 0.11);
}

.register-card__glow {
  position: absolute;
  top: 0;
  right: 0;
  width: 140px;
  height: 140px;
  border-bottom-left-radius: 999px;
  background: linear-gradient(225deg, #dbeafe, transparent 72%);
  pointer-events: none;
}

.register-brand,
.register-heading,
.register-form,
.register-note {
  position: relative;
  z-index: 1;
}

.register-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 22px;
  color: #1d4ed8;
  font-size: 21px;
  font-weight: 900;
}

.register-brand__mark {
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

.register-heading {
  margin-bottom: 24px;
  text-align: center;
}

.register-heading h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 900;
}

.register-heading p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.register-form {
  display: grid;
  gap: 16px;
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

.form-field__hint {
  margin: -2px 0 0;
  font-size: 12px;
  font-weight: 800;
}

.form-field__hint--error {
  color: #b91c1c;
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

.input-clear-button,
.password-toggle {
  position: absolute;
  top: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transform: translateY(-50%);
}

.input-clear-button {
  right: 8px;
  width: 30px;
  height: 30px;
}

.input-clear-button:hover,
.password-toggle:hover {
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
  right: 8px;
  width: 32px;
  height: 32px;
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

.register-submit {
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

.register-submit:hover {
  background: #1d4ed8;
}

.register-submit:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.register-error {
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
}

.register-links {
  display: flex;
  justify-content: center;
  margin-top: -2px;
}

.register-links button {
  border: 0;
  background: transparent;
  color: #2563eb;
  padding: 0;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.register-links button:hover {
  color: #1d4ed8;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.register-submit:focus-visible,
.back-button:focus-visible,
.input-clear-button:focus-visible,
.password-toggle:focus-visible,
.register-links button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.register-note {
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

.register-note--primary {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.register-note__icon {
  display: inline-flex;
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #eff6ff;
}

.register-note__svg {
  display: block;
  width: 18px;
  height: 18px;
  object-fit: contain;
}

@media (max-width: 560px) {
  .register-card {
    padding: 30px 22px;
  }

  .register-topbar {
    padding: 18px;
  }
}
</style>
