import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import 'katex/dist/katex.min.css'
import 'streamdown/styles.css'
import '@nutui/nutui-react/dist/style.css'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider } from '@nutui/nutui-react'
import AppRoutes from '@/routes'
import MobileCheck from '@/components/MobileCheck'
import ViewportHeightSync from '@/components/ViewportHeightSync'

const nutTheme = {
  nutuiColorPrimary: '#15be51',
  nutuiColorPrimaryStop1: '#23d861',
  nutuiColorPrimaryStop2: '#13b84d',
  nutuiColorPrimaryPressed: '#0fa243',
  nutuiButtonPrimaryBorderColor: '#13b84d',
  nutuiBadgeBackgroundColor: '#ff3b30'
} as const

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ViewportHeightSync />
    <MobileCheck>
      <ConfigProvider theme={nutTheme}>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </ConfigProvider>
    </MobileCheck>
  </StrictMode>
)
