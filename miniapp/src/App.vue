<script>
import { useUserStore } from '@/stores/user'

export default {
  onLaunch() {
    // 静默换取登录态：图鉴浏览不需要登录，但收藏/我的搭配需要 token。
    // 失败不阻塞启动，页面在真正需要时会重试。
    const user = useUserStore()
    user.ensureLogin().catch(() => {})
  },
  onShow() {},
  onHide() {},
}
</script>

<style lang="scss">
/* 全局基础样式：只放跨页面复用的重置与通用类，页面私有样式写在各页面内 */
@import '@/styles/tokens.scss';

page {
  background-color: $paper-100;
  color: $ink-800;
  font-family: $font-body;
  font-size: $text-28;
  line-height: $leading-normal;
  -webkit-font-smoothing: antialiased;
}

view,
text,
scroll-view,
image,
input,
textarea,
button {
  box-sizing: border-box;
}

/* ---------- 通用排版 ---------- */
.u-serif {
  font-family: $font-display;
}
.u-title {
  font-family: $font-display;
  font-size: $text-38;
  font-weight: $weight-medium;
  letter-spacing: $tracking-wide;
  color: $ink-900;
}
.u-sub {
  font-size: $text-24;
  color: $ink-400;
}
.u-row {
  display: flex;
  align-items: center;
}
.u-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.u-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.u-clamp-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* ---------- 通用容器 ---------- */
.u-page {
  min-height: 100vh;
  background: $paper-100;
}
.u-pad {
  padding-left: $space-32;
  padding-right: $space-32;
}

/* ---------- 金色细线 ---------- */
.u-hairline {
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(176, 138, 69, 0) 0%,
    rgba(176, 138, 69, 0.45) 50%,
    rgba(176, 138, 69, 0) 100%
  );
}

/* ---------- 按压反馈 ---------- */
.u-press {
  transition: transform $motion-fast $motion-ease, opacity $motion-fast $motion-ease;
}
.u-press:active {
  transform: scale(0.97);
  opacity: 0.86;
}

/* 隐藏小程序默认按钮样式 */
button::after {
  border: none;
}
</style>
