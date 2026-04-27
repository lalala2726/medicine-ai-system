// 触发飞入动画的辅助函数
export const triggerFlyAnimation = (image: string, buttonElement: HTMLElement, onComplete: () => void) => {
  // 获取点击按钮的位置
  const rect = buttonElement.getBoundingClientRect()
  const startX = rect.left + rect.width / 2 - 25
  const startY = rect.top + rect.height / 2 - 25

  // 获取购物车图标的位置
  const cartIcon = document.querySelector('.cart-icon-wrapper') as HTMLElement
  if (!cartIcon) {
    onComplete()
    return
  }

  const cartRect = cartIcon.getBoundingClientRect()
  const endX = cartRect.left + cartRect.width / 2 - 25
  const endY = cartRect.top + cartRect.height / 2 - 25

  // 计算距离，用于动态设置动画参数
  const deltaX = endX - startX
  const deltaY = endY - startY

  // 创建唯一的动画元素
  const flyElement = document.createElement('div')
  const uniqueId = `fly-${Date.now()}-${Math.random()}`
  flyElement.className = 'fly-to-cart-animation'
  flyElement.id = uniqueId

  // 设置基本样式和 CSS 变量
  flyElement.style.cssText = `
    position: fixed;
    left: ${startX}px;
    top: ${startY}px;
    z-index: 9999;
    pointer-events: none;
    --start-x: ${startX}px;
    --start-y: ${startY}px;
    --end-x: ${endX}px;
    --end-y: ${endY}px;
    --delta-x: ${deltaX}px;
    --delta-y: ${deltaY}px;
  `

  const imgElement = document.createElement('img')
  imgElement.src = image
  imgElement.style.cssText = `
    width: 50px;
    height: 50px;
    border-radius: 8px;
  `

  flyElement.appendChild(imgElement)
  document.body.appendChild(flyElement)

  // 使用 requestAnimationFrame 确保动画每次都能触发
  requestAnimationFrame(() => {
    flyElement.classList.add('fly-active')
  })

  setTimeout(() => {
    const element = document.getElementById(uniqueId)
    if (element && document.body.contains(element)) {
      document.body.removeChild(element)
    }
    onComplete()
  }, 850)
}

// 触发购物车图标跳跃动画
export const triggerCartIconJump = () => {
  const cartIcon = document.querySelector('.cart-icon-wrapper') as HTMLElement
  if (!cartIcon) return

  // 添加跳跃动画类
  cartIcon.classList.add('cart-jump-animation')

  // 动画结束后移除类
  setTimeout(() => {
    cartIcon.classList.remove('cart-jump-animation')
  }, 600)
}

// 简化版飞入购物车动画 - 用于商品详情页
export const flyToCart = (event: React.MouseEvent) => {
  const target = event.currentTarget as HTMLElement

  // 创建飞入动画元素
  const flyElement = document.createElement('div')
  const rect = target.getBoundingClientRect()

  flyElement.style.cssText = `
    position: fixed;
    left: ${rect.left + rect.width / 2 - 20}px;
    top: ${rect.top + rect.height / 2 - 20}px;
    width: 40px;
    height: 40px;
    background: #ff4d4f;
    border-radius: 50%;
    z-index: 9999;
    pointer-events: none;
    transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
    opacity: 1;
  `

  document.body.appendChild(flyElement)

  // 触发动画
  requestAnimationFrame(() => {
    flyElement.style.transform = 'translate(-50vw, -50vh) scale(0.2)'
    flyElement.style.opacity = '0'
  })

  // 清理
  setTimeout(() => {
    if (document.body.contains(flyElement)) {
      document.body.removeChild(flyElement)
    }
  }, 600)
}
