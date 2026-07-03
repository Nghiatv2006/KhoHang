<script setup lang="ts">
import { ref, reactive, computed, onBeforeUnmount, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import {
  Scene, WebGLRenderer, PerspectiveCamera, Color, Group, Mesh,
  BoxGeometry, MeshStandardMaterial,
  AmbientLight, DirectionalLight
} from 'three'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const banMsg = ref('') // Thông báo bị phạt spam
const successMsg = ref('') // Thông báo thành công khôi phục mật khẩu
const showPwd = ref(false)

// State cho quy trình Quên mật khẩu
const mode = ref<'login' | 'forgot_email' | 'forgot_select_acc' | 'forgot_otp' | 'forgot_reset'>('login')
const forgotEmail = ref('')
const accountsFound = ref<{ username: string; fullName: string }[]>([])
const selectedUsername = ref('')
const forgotOtp = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

// Bộ đếm thời gian
const otpExpirySeconds = ref(300) // 5 phút hết hạn OTP
const resendCooldown = ref(0)     // Cooldown 30s gửi lại OTP
let expiryIntervalId: any = null
let cooldownIntervalId: any = null

const formattedExpiryTime = computed(() => {
  const mins = Math.floor(otpExpirySeconds.value / 60)
  const secs = otpExpirySeconds.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

function startOtpTimers() {
  clearOtpTimers()

  otpExpirySeconds.value = 300
  resendCooldown.value = 30

  expiryIntervalId = setInterval(() => {
    if (otpExpirySeconds.value > 0) {
      otpExpirySeconds.value--
    } else {
      clearInterval(expiryIntervalId)
      errorMsg.value = 'Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã mới.'
    }
  }, 1000)

  cooldownIntervalId = setInterval(() => {
    if (resendCooldown.value > 0) {
      resendCooldown.value--
    } else {
      clearInterval(cooldownIntervalId)
    }
  }, 1000)
}

function clearOtpTimers() {
  if (expiryIntervalId) clearInterval(expiryIntervalId)
  if (cooldownIntervalId) clearInterval(cooldownIntervalId)
  expiryIntervalId = null
  cooldownIntervalId = null
  otpExpirySeconds.value = 300
  resendCooldown.value = 0
}

let renderer: WebGLRenderer | null = null
let animationFrameId: number | null = null
let handleResizeListener: (() => void) | null = null

onMounted(() => {
  document.documentElement.classList.remove('dark-mode')
})

onBeforeUnmount(() => {
  clearOtpTimers()
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (handleResizeListener) {
    window.removeEventListener('resize', handleResizeListener)
    handleResizeListener = null
  }
  if (renderer) renderer.dispose()
})

function skipIntro() {
  if (!showIntro.value) return
  
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  if (handleResizeListener) {
    window.removeEventListener('resize', handleResizeListener)
    handleResizeListener = null
  }
  if (renderer) {
    renderer.dispose()
    renderer = null
  }
  
  fadeIntro.value = true
  showIntro.value = false
  startAnimations.value = true
  animateUptime()
  animateAes()
}

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ thông tin.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  banMsg.value = ''
  successMsg.value = ''
  try {
    const res = await api.post('/api/auth/login', {
      username: form.username.trim(),
      password: form.password,
    })
    const data = await res.json()
    if (res.ok) {
      localStorage.setItem('wh_user', JSON.stringify(data))
      router.push('/dashboard')
    } else if (res.status === 429) {
      // Bị phạt spam
      const banUntilRaw = data.banUntil
      if (banUntilRaw) {
        const dt = new Date(banUntilRaw).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'medium' })
        banMsg.value = `⚠️ Tài khoản bị tạm khóa do thao tác quá nhanh. Vui lòng thử lại sau: ${dt}`
      } else {
        banMsg.value = data.message || 'Thất bại. Vui lòng thử lại sau.'
      }
    } else {
      errorMsg.value = data.message || 'Tên đăng nhập hoặc mật khẩu không đúng.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ. ' + (err.message || '')
  } finally {
    loading.value = false
  }
}

function enterForgotFlow() {
  clearOtpTimers()
  mode.value = 'forgot_email'
  errorMsg.value = ''
  successMsg.value = ''
  banMsg.value = ''
  forgotEmail.value = ''
  accountsFound.value = []
  selectedUsername.value = ''
  forgotOtp.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

function resetForgotFlow() {
  clearOtpTimers()
  mode.value = 'login'
  forgotEmail.value = ''
  accountsFound.value = []
  selectedUsername.value = ''
  forgotOtp.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  errorMsg.value = ''
}

async function handleFindAccounts() {
  if (!forgotEmail.value.trim()) {
    errorMsg.value = 'Vui lòng nhập email.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.get(`/api/auth/forgot-password/find-accounts?email=${encodeURIComponent(forgotEmail.value.trim())}`)
    const data = await res.json()
    if (res.ok) {
      if (data.length === 0) {
        errorMsg.value = 'Không tìm thấy tài khoản nào liên kết với email này.'
      } else {
        accountsFound.value = data
        if (data.length === 1) {
          selectedUsername.value = data[0].username
          await handleSendOtp()
        } else {
          mode.value = 'forgot_select_acc'
        }
      }
    } else {
      errorMsg.value = data.message || 'Có lỗi xảy ra khi tìm tài khoản.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ.'
  } finally {
    loading.value = false
  }
}

async function handleSendOtp() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/api/auth/forgot-password/send-otp', {
      username: selectedUsername.value,
      email: forgotEmail.value.trim()
    })
    const data = await res.json()
    if (res.ok) {
      mode.value = 'forgot_otp'
      startOtpTimers()
    } else {
      errorMsg.value = data.message || 'Không thể gửi mã OTP.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ.'
  } finally {
    loading.value = false
  }
}

async function handleVerifyOtp() {
  if (!forgotOtp.value.trim()) {
    errorMsg.value = 'Vui lòng nhập mã OTP.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/api/auth/forgot-password/verify-otp', {
      username: selectedUsername.value,
      email: forgotEmail.value.trim(),
      otp: forgotOtp.value.trim()
    })
    const data = await res.json()
    if (res.ok) {
      clearOtpTimers()
      mode.value = 'forgot_reset'
    } else {
      errorMsg.value = data.message || 'Mã OTP không chính xác hoặc đã hết hạn.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ.'
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  if (!newPassword.value) {
    errorMsg.value = 'Vui lòng nhập mật khẩu mới.'
    return
  }
  if (newPassword.value.length < 6) {
    errorMsg.value = 'Mật khẩu phải có ít nhất 6 ký tự.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = 'Mật khẩu xác nhận không khớp.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/api/auth/forgot-password/reset', {
      username: selectedUsername.value,
      otp: forgotOtp.value.trim(),
      newPassword: newPassword.value
    })
    const data = await res.json()
    if (res.ok) {
      clearOtpTimers()
      successMsg.value = 'Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.'
      resetForgotFlow()
    } else {
      errorMsg.value = data.message || 'Đặt lại mật khẩu thất bại.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ.'
  } finally {
    loading.value = false
  }
}

// --- Animation states for metrics ---
const uptimeText = ref("0.00%")
const aesText = ref("AES-256")

function animateUptime() {
  const start = 0
  const end = 99.99
  const duration = 1500 // 1.5 seconds in milliseconds
  const startTime = performance.now()

  function update(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    // Easing out quadratic
    const easeOutQuad = (t: number) => t * (2 - t)
    const easedProgress = easeOutQuad(progress)
    
    const currentValue = start + (end - start) * easedProgress
    uptimeText.value = currentValue.toFixed(2) + "%"

    if (progress < 1) {
      requestAnimationFrame(update)
    } else {
      uptimeText.value = "99.99%"
    }
  }

  requestAnimationFrame(update)
}

function animateAes() {
  const target = "AES-256"
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-="
  
  const queue = target.split("").map((to) => {
    const end = Math.floor(Math.random() * 40) + 30 // Settle between frame 30 and 70
    return {
      to,
      end,
      currentChar: chars[Math.floor(Math.random() * chars.length)]
    }
  })

  let frame = 0
  function update() {
    let output = ""
    let complete = 0
    
    for (let i = 0; i < queue.length; i++) {
      const item = queue[i]
      if (frame >= item.end) {
        output += item.to
        complete++
      } else {
        if (Math.random() < 0.3) {
          item.currentChar = chars[Math.floor(Math.random() * chars.length)]
        }
        output += item.currentChar
      }
    }
    
    aesText.value = output
    
    if (complete < queue.length) {
      frame++
      requestAnimationFrame(update)
    } else {
      aesText.value = target
    }
  }
  
  update()
}

const isShaking = ref(false)
watch([errorMsg, banMsg], ([newErr, newBan]) => {
  if (newErr || newBan) {
    isShaking.value = true
    setTimeout(() => {
      isShaking.value = false
    }, 500)
  }
})

const startAnimations = ref(false)
const showIntro = ref(true)
const fadeIntro = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// Easing functions
function easeInQuad(t: number) { return t * t }
function easeOutQuad(t: number) { return t * (2 - t) }
function easeInOutQuad(t: number) { return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t }
// Removed unused easeOutBack
function easeOutSine(t: number) { return Math.sin(t * Math.PI / 2) }
function easeInCubic(t: number) { return t * t * t }
function easeOutElastic(t: number) {
  if (t === 0 || t === 1) return t
  return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI) / 3) + 1
}
function lerp(a: number, b: number, t: number) { return a + (b - a) * t }
function clamp01(t: number) { return Math.max(0, Math.min(1, t)) }
function progress(elapsed: number, start: number, end: number) {
  return clamp01((elapsed - start) / (end - start))
}

interface TileState {
  ox: number; oz: number; dist: number; isCenter: boolean
  crashY: number; sinkY: number; hoverY: number
  flyX: number; flyZ: number; flyY: number
  rotX: number; rotY: number; rotZ: number
  flyDelay: number
}

function init3DAnimation() {
  if (!canvasRef.value) return

  const width = window.innerWidth
  const height = window.innerHeight

  const scene = new Scene()
  scene.background = new Color('#ffffff')

  const camera = new PerspectiveCamera(35, width / height, 0.1, 200)
  camera.position.set(0, 5.64, 20.66)
  camera.rotation.x = -14.73 * (Math.PI / 180)

  renderer = new WebGLRenderer({ canvas: canvasRef.value, antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(new Color('#ffffff'), 1)

  scene.add(new AmbientLight(0xffffff, 0.6))
  const dirLight = new DirectionalLight(0xffffff, 1.6)
  dirLight.position.set(100, 200, 300)
  scene.add(dirLight)
  const rimLight = new DirectionalLight(0xaab8ff, 0.6)
  rimLight.position.set(-200, -50, 150)
  scene.add(rimLight)

  // ─── Floor tiles ───────────────────────────────────────────────
  const COLS = 31, ROWS = 31
  // Removed unused EXPLODE_RADIUS
  const centerCol = Math.floor(COLS / 2)
  const centerRow = Math.floor(ROWS / 2)

  const tileGeo = new BoxGeometry(0.94, 0.94, 0.94)
  const tileMat = new MeshStandardMaterial({ color: new Color('#d4d4d8'), roughness: 0.7, metalness: 0.05 })

  const tileGroup = new Group()
  const tileMeshes: Mesh[] = []
  const tileStates: TileState[] = []

  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const mesh = new Mesh(tileGeo, tileMat)
      const ox = c - centerCol
      const oz = r - centerRow
      mesh.position.set(ox, 0, oz)
      tileGroup.add(mesh)
      tileMeshes.push(mesh)

      const dist = Math.hypot(ox, oz)
      const crashY = Math.max(0, (1 - dist / 22)) * 0.4
      const baseSink = lerp(2.0, 0.4, dist / 22)
      // Scatter tiles to float at random suspended heights during the shatter phase
      const hoverY = (Math.random() - 0.5) * 3.5 - (dist / 22) * 1.0
      const spreadFactor = lerp(6.0, 1.2, dist / 22)
      const angle = Math.atan2(oz, ox)
      tileStates.push({
        ox, oz, dist, isCenter: true,
        crashY,
        sinkY: -baseSink * 0.35,
        hoverY,
        flyX: Math.cos(angle) * spreadFactor * (2 + Math.random() * 3) + (Math.random() - 0.5) * 4,
        flyZ: Math.sin(angle) * spreadFactor * (2 + Math.random() * 3) + (Math.random() - 0.5) * 4 + 8,
        flyY: 8 + Math.random() * 16,
        rotX: (Math.random() - 0.5) * Math.PI * 4,
        rotY: (Math.random() - 0.5) * Math.PI * 4,
        rotZ: (Math.random() - 0.5) * Math.PI * 4,
        flyDelay: (dist / 22) * 0.4,
      })
    }
  }
  scene.add(tileGroup)

  // ─── Falling box (starts as a tall thin needle) ───────────────
  const boxGeo = new BoxGeometry(1, 1, 1)
  const boxMat = new MeshStandardMaterial({ color: new Color('#FF4B4B'), roughness: 0.4, metalness: 0.1 })
  const box = new Mesh(boxGeo, boxMat)
  box.position.set(0, 24, 0)
  box.scale.set(0.02, 5, 0.02)
  scene.add(box)

  handleResizeListener = () => {
    if (!renderer) return
    camera.aspect = window.innerWidth / window.innerHeight
    camera.updateProjectionMatrix()
    renderer.setSize(window.innerWidth, window.innerHeight)
  }
  window.addEventListener('resize', handleResizeListener)

  // ─── Timeline constants (ms) ──────────────────────────────────
  const T = {
    FALL_START:     0,
    FALL_END:    3000,
    IMPACT:      3000,
    RIPPLE_END:  3500,
    SQUASH_END:  4000,
    FLOOR_SINK:  3500,
    FLOOR_SINK_END: 5500,
    BOX_STRETCH: 5500,
    BOX_LAUNCH:  6500,
    BOX_GONE:    7200,
    FLY_START:   6600,
    FLY_END:    10300,
    FADE_START:  8800,
    TOTAL:      11000,
  }

  let cameraShakeAmp = 0
  const startTime = performance.now()
  let done = false

  const tick = () => {
    if (done || !renderer) return
    const elapsed = performance.now() - startTime

    // 1. ANIMATE RED BOX
    let bottomOffset = 0
    if (elapsed >= 4000 && elapsed < 4400) {
      // Jump up: box lifts 1.8 units in the air as a solid body
      const ct = progress(elapsed, 4000, 4400)
      bottomOffset = lerp(0.0, 1.8, easeOutQuad(ct))
    } else if (elapsed >= 4400 && elapsed < 4500) {
      // Stomp down: box slams down to the ground rapidly
      const st = progress(elapsed, 4400, 4500)
      bottomOffset = lerp(1.8, 0.0, easeInQuad(st))
    }

    if (elapsed <= T.FALL_END) {
      const ft = progress(elapsed, T.FALL_START, T.FALL_END)
      const eased = easeInCubic(ft)
      box.position.y = lerp(24, 1.47, eased)
      const sxz = lerp(0.02, 1, eased)
      box.scale.x = sxz
      box.scale.z = sxz
      box.scale.y = lerp(5, 1, eased)
    } else if (elapsed >= 3000 && elapsed < 4000) {
      const ft = progress(elapsed, 3000, 4000)
      const elastic = easeOutElastic(ft)
      box.scale.y = lerp(0.55, 1, elastic)
      box.scale.x = lerp(1.45, 1, elastic)
      box.scale.z = lerp(1.45, 1, elastic)
    } else if (elapsed >= 4000 && elapsed < 5500) {
      // Keep box rigid (scale 1x1x1) during jump, stomp, and initial sink
      box.scale.set(1, 1, 1)
    } else if (elapsed >= 5500 && elapsed < 6500) {
      const st = progress(elapsed, 5500, 6500)
      const eased = easeInOutQuad(st)
      box.scale.y = lerp(1, 4, eased)
      box.scale.x = lerp(1, 0.6, eased)
      box.scale.z = lerp(1, 0.6, eased)
    } else if (elapsed >= 6500 && elapsed < 7200) {
      const lt = progress(elapsed, 6500, 7200)
      const eased = easeOutSine(lt)
      box.scale.y = lerp(4, 0.1, eased)
      box.scale.x = lerp(0.6, 0.01, eased)
      box.scale.z = lerp(0.6, 0.01, eased)
    }
    if (elapsed >= 7200) {
      box.visible = false
    }

    // 2. ANIMATE TILES (Unified Loop)
    for (let i = 0; i < tileMeshes.length; i++) {
      const ts = tileStates[i]
      let tx = ts.ox
      let tz = ts.oz
      let ty = 0
      let rx = 0
      let ry = 0
      let rz = 0

      if (elapsed < 3000) {
        ty = 0
      } else if (elapsed >= 3000 && elapsed < 4000) {
        // Sudden drop/sag within 200ms on first impact
        const ft = progress(elapsed, 3000, 3200)
        const tileDelay = (ts.dist / 22) * 0.08
        const lt = clamp01((ft - tileDelay) / (1 - tileDelay + 0.01))
        const eased = easeOutQuad(lt)

        ty = lerp(0, ts.sinkY, eased)
        rx = lerp(0, ts.rotX * 0.05, eased)
        ry = lerp(0, ts.rotY * 0.05, eased)
        rz = lerp(0, ts.rotZ * 0.05, eased)
        tx = lerp(ts.ox, ts.ox * 1.05, eased)
        tz = lerp(ts.oz, ts.oz * 1.05, eased)
      } else if (elapsed >= 4000 && elapsed < 4500) {
        ty = ts.sinkY
        rx = ts.rotX * 0.05
        ry = ts.rotY * 0.05
        rz = ts.rotZ * 0.05
        tx = ts.ox * 1.05
        tz = ts.oz * 1.05
      } else if (elapsed >= 4500 && elapsed < 5500) {
        // Explosive collapse/shatter within 200ms upon stomp impact
        const ft = progress(elapsed, 4500, 4700)
        const tileDelay = (ts.dist / 22) * 0.05
        const lt = clamp01((ft - tileDelay) / (1 - tileDelay + 0.01))
        const eased = easeOutQuad(lt)

        ty = lerp(ts.sinkY, ts.hoverY, eased)
        rx = lerp(ts.rotX * 0.05, ts.rotX * 0.25, eased)
        ry = lerp(ts.rotY * 0.05, ts.rotY * 0.25, eased)
        rz = lerp(ts.rotZ * 0.05, ts.rotZ * 0.25, eased)
        tx = lerp(ts.ox * 1.05, ts.ox * 1.25, eased)
        tz = lerp(ts.oz * 1.05, ts.oz * 1.25, eased)
      } else if (elapsed >= 5500 && elapsed < 6600) {
        ty = ts.hoverY
        rx = ts.rotX * 0.25
        ry = ts.rotY * 0.25
        rz = ts.rotZ * 0.25
        tx = ts.ox * 1.25
        tz = ts.oz * 1.25
      } else if (elapsed >= 6600) {
        const ft = progress(elapsed, 6600, 10300)
        const lt = clamp01((ft - ts.flyDelay) / (1 - ts.flyDelay + 0.01))
        const eased = easeOutSine(lt)

        ty = lerp(ts.hoverY, ts.flyY, eased)
        tx = lerp(ts.ox * 1.25, ts.flyX, eased)
        tz = lerp(ts.oz * 1.25, ts.flyZ, eased)
        rx = lerp(ts.rotX * 0.25, ts.rotX * 0.75, eased)
        ry = lerp(ts.rotY * 0.25, ts.rotY * 0.75, eased)
        rz = lerp(ts.rotZ * 0.25, ts.rotZ * 0.75, eased)
      }

      // Continuous zero-gravity slow drifting once shattered
      if (elapsed >= 4500) {
        const tOffset = (elapsed - 4500) * 0.001
        ty += Math.sin(tOffset * 2.2 + ts.dist * 0.5) * 0.25
        tx += Math.cos(tOffset * 1.8 + ts.ox * 0.3) * 0.15
        tz += Math.sin(tOffset * 1.6 + ts.oz * 0.3) * 0.15
        rx += Math.sin(tOffset * 1.2 + ts.rotX) * 0.08
        ry += Math.cos(tOffset * 1.0 + ts.rotY) * 0.08
        rz += Math.sin(tOffset * 0.8 + ts.rotZ) * 0.08
      }

      tileMeshes[i].position.set(tx, ty, tz)
      tileMeshes[i].rotation.set(rx, ry, rz)
    }

    // 3. ANCHOR BOX POSITION TO CENTER TILE 480
    if (elapsed >= 3000 && elapsed < 5500) {
      const centerTileMesh = tileMeshes[480]
      const floorY = centerTileMesh.position.y + 0.47
      box.position.y = floorY + box.scale.y + bottomOffset
    } else if (elapsed >= 5500 && elapsed < 6500) {
      const st = progress(elapsed, 5500, 6500)
      const eased = easeInOutQuad(st)
      const centerTileMesh = tileMeshes[480]
      const startY = centerTileMesh.position.y + 0.47 + 1.0
      box.position.y = lerp(startY, 4, eased)
    } else if (elapsed >= 6500 && elapsed < 7200) {
      const lt = progress(elapsed, 6500, 7200)
      const eased = easeOutSine(lt)
      box.position.y = lerp(4, 50, eased)
    }

    // Camera shake trigger & decay
    if (elapsed >= 3000 && elapsed < 4000) {
      cameraShakeAmp = lerp(0.25, 0, progress(elapsed, 3000, 4000))
    }
    if (elapsed >= 4500 && elapsed < 4530) {
      cameraShakeAmp = 0.25
    }
    if (cameraShakeAmp > 0.01) {
      camera.position.y += (Math.random() - 0.5) * cameraShakeAmp
      cameraShakeAmp *= 0.85
    }

    // ═══ Fade canvas out ═════════════════════════════════════════
    // Transition trigger
    if (elapsed >= T.FADE_START && !fadeIntro.value) {
      fadeIntro.value = true
      startAnimations.value = true
      animateUptime()
      animateAes()
    }

    renderer.render(scene, camera)

    if (elapsed >= T.TOTAL) {
      done = true
      showIntro.value = false
      if (handleResizeListener) {
        window.removeEventListener('resize', handleResizeListener)
        handleResizeListener = null
      }
      renderer.dispose()
      renderer = null
      return
    }

    animationFrameId = requestAnimationFrame(tick)
  }

  animationFrameId = requestAnimationFrame(tick)
}

onMounted(async () => {
  await nextTick()
  if (canvasRef.value) {
    init3DAnimation()
  }
})
</script>

<template>
  <div class="min-h-screen flex relative font-['Nunito',sans-serif] overflow-hidden bg-[#0a192f]">
    
    <!-- 3D Intro Canvas -->
    <canvas 
      v-if="showIntro" 
      ref="canvasRef" 
      @click="skipIntro"
      class="fixed inset-0 w-full h-full z-50 transition-opacity duration-1000 cursor-pointer" 
      style="background-color: #ffffff;"
      :class="{ 'opacity-0 pointer-events-none': fadeIntro }"
    ></canvas>
    
    <!-- Global Animated Background -->
    <div class="absolute inset-0 z-0">
      <img 
        src="https://images.unsplash.com/photo-1553413077-190dd305871c?q=80&w=2070&auto=format&fit=crop" 
        alt="Enterprise Warehouse" 
        class="w-full h-full object-cover scale-105 animate-slow-pan opacity-70 mix-blend-luminosity"
      />
    </div>

    <!-- Left Panel Overlay & Content (Form) -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-4 relative z-10">
      
      <!-- Lớp phủ 30% -->
      <div class="absolute inset-0 bg-black/30 backdrop-blur-[2px] border-r border-white/10"></div>
      
      <!-- Login Card -->
      <div 
        class="relative z-20 bg-white/95 backdrop-blur-xl rounded-[20px] shadow-[0_15px_40px_rgba(0,0,0,0.1)] p-8 md:p-12 w-full max-w-[480px] border border-white/60 transition-transform duration-300"
        :class="{ 'animate-shake': isShaking }"
      >
        
        <!-- Logo & Header -->
        <div class="text-center mb-8">
          <div class="relative w-[84px] h-[84px] mx-auto mb-4 flex items-center justify-center group cursor-pointer">
            <!-- Squircle Accent Background (Rotates 45deg on hover) -->
            <div class="absolute inset-0 rounded-[24px] bg-gradient-to-tr from-[#d63031] to-[#ff7675] opacity-10 group-hover:opacity-20 group-hover:rotate-45 transition-all duration-700"></div>
            
            <!-- Glass Card Logo Body (Lifts on hover) -->
            <div class="w-[64px] h-[64px] rounded-[20px] bg-gradient-to-tr from-white to-slate-50 border border-slate-100 shadow-[0_8px_30px_rgba(0,0,0,0.06)] flex items-center justify-center relative overflow-hidden group-hover:shadow-[0_15px_35px_rgba(214,48,49,0.15)] group-hover:-translate-y-1 transition-all duration-300">
              <!-- Diagonal shiny reflection sweep -->
              <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-out"></div>
              
              <!-- Custom SVG Logo with falling cubes (Only in login mode) -->
              <svg v-if="mode === 'login'" class="w-[58px] h-[58px] relative z-10" viewBox="20 32 60 51" fill="none" xmlns="http://www.w3.org/2000/svg">
                <defs>
                  <linearGradient id="cardboardGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#f5d6a7" />
                    <stop offset="100%" stop-color="#d4a373" />
                  </linearGradient>
                  <linearGradient id="tapeGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#ff7675" />
                    <stop offset="100%" stop-color="#d63031" />
                  </linearGradient>
                </defs>
                <!-- Cube 1 (Bottom Left) -->
                <g :class="startAnimations ? 'cube-1' : 'opacity-0'">
                  <!-- Top Face -->
                  <path d="M 0 -12 L 14 -5 L 0 2 L -14 -5 Z" fill="#f5d6a7" stroke="#a0784c" stroke-width="0.8" />
                  <!-- Left Face -->
                  <path d="M -14 -5 L 0 2 L 0 16 L -14 9 Z" fill="#d4a373" stroke="#a0784c" stroke-width="0.8" />
                  <!-- Right Face -->
                  <path d="M 0 2 L 14 -5 L 14 9 L 0 16 Z" fill="#bfa181" stroke="#a0784c" stroke-width="0.8" />
                  <!-- Top Face Center Seam -->
                  <line x1="0" y1="-12" x2="0" y2="2" stroke="#a0784c" stroke-width="0.8" />
                  <!-- Red Packaging Tape (covering the seam) -->
                  <path d="M -2.5 -10.75 L 2.5 -13.25 L 2.5 3.25 L -2.5 0.75 Z" fill="url(#tapeGrad)" />
                  <path d="M -2.5 0.75 L 0 2 L 0 16 L -2.5 14.75 Z" fill="url(#tapeGrad)" />
                  <path d="M 0 2 L 2.5 0.75 L 2.5 14.75 L 0 16 Z" fill="url(#tapeGrad)" />
                </g>
                <!-- Cube 2 (Bottom Right) -->
                <g :class="startAnimations ? 'cube-2' : 'opacity-0'">
                  <path d="M 0 -12 L 14 -5 L 0 2 L -14 -5 Z" fill="#f5d6a7" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M -14 -5 L 0 2 L 0 16 L -14 9 Z" fill="#d4a373" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M 0 2 L 14 -5 L 14 9 L 0 16 Z" fill="#bfa181" stroke="#a0784c" stroke-width="0.8" />
                  <line x1="0" y1="-12" x2="0" y2="2" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M -2.5 -10.75 L 2.5 -13.25 L 2.5 3.25 L -2.5 0.75 Z" fill="url(#tapeGrad)" />
                  <path d="M -2.5 0.75 L 0 2 L 0 16 L -2.5 14.75 Z" fill="url(#tapeGrad)" />
                  <path d="M 0 2 L 2.5 0.75 L 2.5 14.75 L 0 16 Z" fill="url(#tapeGrad)" />
                </g>
                <!-- Cube 3 (Top Center) -->
                <g :class="startAnimations ? 'cube-3' : 'opacity-0'">
                  <path d="M 0 -12 L 14 -5 L 0 2 L -14 -5 Z" fill="#f5d6a7" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M -14 -5 L 0 2 L 0 16 L -14 9 Z" fill="#d4a373" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M 0 2 L 14 -5 L 14 9 L 0 16 Z" fill="#bfa181" stroke="#a0784c" stroke-width="0.8" />
                  <line x1="0" y1="-12" x2="0" y2="2" stroke="#a0784c" stroke-width="0.8" />
                  <path d="M -2.5 -10.75 L 2.5 -13.25 L 2.5 3.25 L -2.5 0.75 Z" fill="url(#tapeGrad)" />
                  <path d="M -2.5 0.75 L 0 2 L 0 16 L -2.5 14.75 Z" fill="url(#tapeGrad)" />
                  <path d="M 0 2 L 2.5 0.75 L 2.5 14.75 L 0 16 Z" fill="url(#tapeGrad)" />
                </g>
              </svg>
              
              <!-- FontAwesome Icon for other modes -->
              <i 
                v-else
                :class="[
                  'fas', 
                  mode === 'forgot_email' ? 'fa-envelope-open-text' : mode === 'forgot_select_acc' ? 'fa-users-cog' : mode === 'forgot_otp' ? 'fa-key' : 'fa-shield-alt', 
                  'fa-2x', 
                  'bg-gradient-to-tr from-[#d63031] to-[#ff7675] bg-clip-text text-transparent transform group-hover:scale-110 transition-transform duration-300'
                ]"
              ></i>
            </div>
          </div>
          <h2 class="text-[32px] font-extrabold text-gray-900 mb-2">
            <template v-if="mode === 'login'">
              <span class="pro-gradient-text">Warehouse Pro</span>
            </template>
            <template v-else>
              {{ mode === 'forgot_email' ? 'Quên mật khẩu' : mode === 'forgot_select_acc' ? 'Chọn tài khoản' : mode === 'forgot_otp' ? 'Xác minh OTP' : 'Đặt mật khẩu mới' }}
            </template>
          </h2>
          <p class="text-gray-500 font-medium">
            {{ mode === 'login' ? 'Đăng nhập hệ thống quản trị' : mode === 'forgot_email' ? 'Nhập email để khôi phục tài khoản' : mode === 'forgot_select_acc' ? 'Chọn tài khoản muốn khôi phục' : mode === 'forgot_otp' ? 'Nhập mã xác thực gửi qua email' : 'Thiết lập mật khẩu bảo mật mới' }}
          </p>
        </div>

        <!-- Alert Messages -->
        <div v-if="successMsg" class="bg-emerald-50 border border-emerald-300 text-emerald-800 px-4 py-3 rounded-xl shadow-sm mb-6 flex items-start gap-3 text-sm font-semibold">
          <i class="fas fa-check-circle text-emerald-500 text-lg mt-0.5 shrink-0"></i>
          <span>{{ successMsg }}</span>
        </div>
        <div v-if="banMsg" class="bg-amber-50 border border-amber-300 text-amber-800 px-4 py-3 rounded-xl shadow-sm mb-6 flex items-start gap-3 text-sm font-semibold">
          <i class="fas fa-ban text-amber-500 text-lg mt-0.5 shrink-0"></i>
          <span>{{ banMsg }}</span>
        </div>
        <div v-else-if="errorMsg" class="bg-[#f8d7da] text-[#721c24] px-4 py-3 rounded-xl shadow-sm border-0 mb-6 flex items-center gap-3 text-sm font-semibold">
          <i class="fas fa-exclamation-triangle text-lg"></i> <span>{{ errorMsg }}</span>
        </div>

        <!-- Form Đăng nhập -->
        <form v-if="mode === 'login'" @submit.prevent="handleLogin">
          <!-- Smooth Custom Floating Label: Username -->
          <div class="form-floating-custom mb-5">
            <input 
              type="text" 
              id="usernameInput" 
              v-model="form.username" 
              placeholder=" " 
              required 
              autofocus
            />
            <label for="usernameInput">Tên đăng nhập</label>
            <i class="fas fa-user icon"></i>
          </div>

          <!-- Smooth Custom Floating Label: Password -->
          <div class="form-floating-custom mb-2">
            <input 
              :type="showPwd ? 'text' : 'password'" 
              id="passwordInput" 
              v-model="form.password" 
              placeholder=" " 
              required 
              style="padding-right: 3rem;"
            />
            <label for="passwordInput">Mật khẩu</label>
            <i class="fas fa-lock icon"></i>
            <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#d63031] transition-colors z-10" @click="showPwd = !showPwd">
              <i :class="['fas', showPwd ? 'fa-eye-slash' : 'fa-eye', 'text-lg']"></i>
            </button>
          </div>

          <!-- Nút Quên mật khẩu -->
          <a href="#" @click.prevent="enterForgotFlow" class="block text-right mt-3 mb-6 text-[#d63031] font-bold text-[0.95rem] transition-all hover:text-[#b00000] hover:underline">
            Quên mật khẩu?
          </a>

          <!-- Nút Đăng nhập -->
          <button 
            type="submit" 
            :disabled="loading"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-sign-in-alt"></i>
            {{ loading ? 'ĐANG XỬ LÝ...' : 'ĐĂNG NHẬP NGAY' }}
          </button>
        </form>

        <!-- Form Nhập Email -->
        <form v-else-if="mode === 'forgot_email'" @submit.prevent="handleFindAccounts">
          <div class="form-floating-custom mb-6">
            <input 
              type="email" 
              id="forgotEmailInput" 
              v-model="forgotEmail" 
              placeholder=" " 
              required 
              autofocus
            />
            <label for="forgotEmailInput">Email liên kết</label>
            <i class="fas fa-envelope icon"></i>
          </div>

          <button 
            type="submit" 
            :disabled="loading"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-search"></i>
            {{ loading ? 'ĐANG TÌM KIẾM...' : 'TÌM TÀI KHOẢN' }}
          </button>

          <button 
            type="button" 
            @click="resetForgotFlow"
            class="w-full mt-4 text-center text-gray-500 hover:text-gray-700 font-bold transition-all text-[0.95rem]"
          >
            Quay lại Đăng nhập
          </button>
        </form>

        <!-- Form Chọn tài khoản -->
        <div v-else-if="mode === 'forgot_select_acc'">
          <div class="space-y-3 mb-6 max-h-[220px] overflow-y-auto pr-1">
            <div 
              v-for="acc in accountsFound" 
              :key="acc.username" 
              @click="selectedUsername = acc.username"
              :class="['p-4 rounded-xl border-2 cursor-pointer transition-all flex items-center justify-between', selectedUsername === acc.username ? 'border-[#d63031] bg-red-50/30' : 'border-gray-200 hover:border-gray-300']"
            >
              <div>
                <p class="font-bold text-gray-800">{{ acc.fullName }}</p>
                <p class="text-sm text-gray-500">Tên đăng nhập: @{{ acc.username }}</p>
              </div>
              <i v-if="selectedUsername === acc.username" class="fas fa-check-circle text-[#d63031] text-lg"></i>
            </div>
          </div>

          <button 
            type="button" 
            @click="handleSendOtp"
            :disabled="!selectedUsername || loading"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-paper-plane"></i>
            {{ loading ? 'ĐANG GỬI OTP...' : 'GỬI MÃ OTP' }}
          </button>

          <button 
            type="button" 
            @click="mode = 'forgot_email'"
            class="w-full mt-4 text-center text-gray-500 hover:text-gray-700 font-bold transition-all text-[0.95rem]"
          >
            Quay lại nhập Email
          </button>
        </div>

        <!-- Form Nhập OTP -->
        <form v-else-if="mode === 'forgot_otp'" @submit.prevent="handleVerifyOtp">
          <p class="text-sm text-gray-600 text-center mb-4 leading-relaxed">
            Mã OTP đã được gửi đến email <span class="font-semibold text-gray-800">{{ forgotEmail }}</span>. Vui lòng kiểm tra hộp thư.
          </p>

          <!-- OTP Expiry Timer Display -->
          <div class="text-center mb-6">
            <span v-if="otpExpirySeconds > 0" class="text-xs font-bold text-gray-500 bg-gray-100 px-3 py-1.5 rounded-full inline-flex items-center gap-1.5">
              <i class="far fa-clock text-amber-500"></i>
              Mã hết hạn sau: <span class="text-[#d63031] font-mono font-extrabold">{{ formattedExpiryTime }}</span>
            </span>
            <span v-else class="text-xs font-bold text-red-600 bg-red-50 border border-red-200 px-3 py-1.5 rounded-full inline-flex items-center gap-1.5">
              <i class="fas fa-exclamation-circle text-red-500"></i>
              Mã OTP đã hết hạn
            </span>
          </div>

          <div class="form-floating-custom mb-6">
            <input 
              type="text" 
              id="otpInput" 
              v-model="forgotOtp" 
              placeholder=" " 
              maxlength="6" 
              required 
              autofocus
            />
            <label for="otpInput">Nhập mã OTP (6 chữ số)</label>
            <i class="fas fa-key icon"></i>
          </div>

          <button 
            type="submit" 
            :disabled="loading || otpExpirySeconds <= 0"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-shield-alt"></i>
            {{ loading ? 'ĐANG XÁC MINH...' : 'XÁC NHẬN OTP' }}
          </button>

          <div class="flex justify-between items-center mt-4 px-1">
            <button 
              type="button" 
              @click="handleSendOtp" 
              :disabled="loading || resendCooldown > 0"
              class="text-[#d63031] font-bold text-sm hover:underline disabled:opacity-50 disabled:no-underline disabled:text-gray-400"
            >
              {{ resendCooldown > 0 ? `Gửi lại OTP sau ${resendCooldown}s` : 'Gửi lại OTP' }}
            </button>
            <button 
              type="button" 
              @click="mode = 'forgot_email'"
              class="text-gray-500 hover:text-gray-700 font-bold text-sm"
            >
              Quay lại từ đầu
            </button>
          </div>
        </form>

        <!-- Form Đặt lại mật khẩu -->
        <form v-else-if="mode === 'forgot_reset'" @submit.prevent="handleResetPassword">
          <p class="text-sm text-gray-600 mb-6 text-center leading-relaxed">
            Thiết lập mật khẩu mới cho tài khoản <span class="font-semibold text-gray-800">@{{ selectedUsername }}</span>.
          </p>
          
          <div class="form-floating-custom mb-5">
            <input 
              type="password" 
              id="newPasswordInput" 
              v-model="newPassword" 
              placeholder=" " 
              required 
              autofocus
            />
            <label for="newPasswordInput">Mật khẩu mới</label>
            <i class="fas fa-lock icon"></i>
          </div>

          <div class="form-floating-custom mb-6">
            <input 
              type="password" 
              id="confirmPasswordInput" 
              v-model="confirmPassword" 
              placeholder=" " 
              required 
            />
            <label for="confirmPasswordInput">Xác nhận mật khẩu</label>
            <i class="fas fa-check-double icon"></i>
          </div>

          <button 
            type="submit" 
            :disabled="loading"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-save"></i>
            {{ loading ? 'ĐANG LƯU MẬT KHẨU...' : 'ĐẶT LẠI MẬT KHẨU' }}
          </button>
          
          <button 
            type="button" 
            @click="resetForgotFlow"
            class="w-full mt-4 text-center text-gray-500 hover:text-gray-700 font-bold transition-all text-[0.95rem]"
          >
            Hủy bỏ & Đăng nhập
          </button>
        </form>

      </div>
    </div>

    <!-- Right Panel Overlay & Content -->
    <div class="hidden lg:flex flex-1 relative items-center justify-center z-10 overflow-hidden">
      
      <!-- Dark Blue Enterprise Overlay -->
      <div class="absolute inset-0 bg-gradient-to-tr from-[#0a192f]/95 via-[#112240]/85 to-[#0047b3]/40 mix-blend-multiply z-0"></div>

      <!-- Professional Content -->
      <div class="relative z-20 px-16 max-w-2xl text-left border-l-4 border-[#00a8ff] pl-8 ml-8">
        <div class="text-[#00a8ff] mb-4 uppercase tracking-[0.2em] text-sm font-extrabold flex items-center gap-2 animate-fade-in-up">
          <i class="fas fa-server"></i> Phiên bản Doanh nghiệp
        </div>

        <h2 class="text-[3.5rem] font-extrabold mb-6 tracking-tight text-white leading-[1.15] animate-fade-in-up delay-150">
          Kiểm soát toàn diện <br/>
          <span class="pro-gradient-text">chuỗi cung ứng</span>
        </h2>
        
        <p class="text-[1.15rem] text-slate-300 font-light leading-relaxed mb-10 animate-fade-in-up delay-300">
          Hệ thống lõi cung cấp khả năng hiển thị thời gian thực, quản lý tồn kho đa chi nhánh và tối ưu hóa luồng hàng hóa với độ trễ bằng 0.
        </p>

        <div class="grid grid-cols-2 gap-8 pt-8 border-t border-white/10 animate-fade-in-up delay-450">
          <div class="p-4 rounded-xl bg-transparent border border-transparent hover:bg-white/5 hover:border-white/10 hover:-translate-y-1 transition-all duration-300 cursor-default group">
            <div class="text-3xl font-bold mb-1 font-mono tracking-tight text-white group-hover:text-[#00a8ff] transition-colors duration-300">{{ uptimeText }}</div>
            <div class="text-sm font-semibold text-slate-400 uppercase tracking-wide">Uptime Hệ thống</div>
          </div>
          <div class="p-4 rounded-xl bg-transparent border border-transparent hover:bg-white/5 hover:border-white/10 hover:-translate-y-1 transition-all duration-300 cursor-default group">
            <div class="text-3xl font-bold mb-1 font-mono tracking-tight text-white group-hover:text-[#00a8ff] transition-colors duration-300">{{ aesText }}</div>
            <div class="text-sm font-semibold text-slate-400 uppercase tracking-wide">Mã hóa Dữ liệu</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;500;600;700;800&display=swap');

/* --- CUSTOM SMOOTH FLOATING LABELS --- */
.form-floating-custom {
  position: relative;
  width: 100%;
}

.form-floating-custom input {
  width: 100%;
  height: 55px;
  border-radius: 10px;
  border: 1px solid #dfe6e9;
  padding: 1.25rem 1rem 0.25rem 2.8rem; /* pb is smaller to push text up slightly, pt is larger */
  font-size: 1rem;
  font-weight: 600;
  color: #2d3436;
  outline: none;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
  background-color: transparent;
}

.form-floating-custom input:focus {
  border-color: #d63031;
  box-shadow: 0 0 0 0.25rem rgba(214, 48, 49, 0.15);
}

.form-floating-custom label {
  position: absolute;
  top: 0;
  left: 2.8rem;
  height: 100%;
  padding: 1rem 0;
  pointer-events: none;
  transform-origin: 0 0;
  transition: opacity 0.15s ease-in-out, transform 0.15s ease-in-out;
  color: #636e72;
  font-weight: 600;
  font-size: 1rem;
}

.form-floating-custom input:focus ~ label,
.form-floating-custom input:not(:placeholder-shown) ~ label {
  opacity: 0.7;
  transform: scale(0.8) translateY(-0.65rem);
}

.form-floating-custom .icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: #b2bec3;
  font-size: 1.1rem;
  transition: color 0.15s ease-in-out;
  pointer-events: none;
}

.form-floating-custom input:focus ~ .icon {
  color: #d63031;
}

/* Fix input autofill background */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active{
    -webkit-box-shadow: 0 0 0 50px white inset !important;
    -webkit-text-fill-color: #2d3436 !important;
}

/* --- PROFESSIONAL RIGHT PANEL ANIMATIONS --- */
@keyframes slowPan {
  0% { transform: scale(1.0) translate(0, 0); }
  100% { transform: scale(1.15) translate(-5%, 2%); }
}

.animate-slow-pan {
  animation: slowPan 8s ease-in-out infinite alternate;
}

.pro-gradient-text {
  background: linear-gradient(to right, #00a8ff, #9c88ff, #fbc531, #e84118, #00a8ff);
  background-size: 300% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: transparent;
  animation: shine 12s ease-in-out infinite alternate;
}

@keyframes shine {
  to {
    background-position: 300% center;
  }
}

/* --- NEW PREMIUM ANIMATIONS & EFFECTS --- */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.delay-150 {
  animation-delay: 150ms;
}

.delay-300 {
  animation-delay: 300ms;
}

.delay-450 {
  animation-delay: 450ms;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-6px); }
  40%, 80% { transform: translateX(6px); }
}

.animate-shake {
  animation: shake 0.5s ease-in-out;
}

/* --- CUBE FALLING ANIMATION --- */
.cube-1 {
  opacity: 0;
  transform: translate(35px, 65px);
  animation: dropCube1 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  animation-delay: 0.2s;
}

.cube-2 {
  opacity: 0;
  transform: translate(65px, 65px);
  animation: dropCube2 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  animation-delay: 0.5s;
}

.cube-3 {
  opacity: 0;
  transform: translate(50px, 46px);
  animation: dropCube3 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  animation-delay: 0.8s;
}

@keyframes dropCube1 {
  0% {
    opacity: 0;
    transform: translate(35px, -60px);
  }
  100% {
    opacity: 1;
    transform: translate(35px, 65px);
  }
}

@keyframes dropCube2 {
  0% {
    opacity: 0;
    transform: translate(65px, -60px);
  }
  100% {
    opacity: 1;
    transform: translate(65px, 65px);
  }
}

@keyframes dropCube3 {
  0% {
    opacity: 0;
    transform: translate(50px, -60px);
  }
  100% {
    opacity: 1;
    transform: translate(50px, 46px);
  }
}
</style>
