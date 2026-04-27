import { Icon } from '@iconify/react'
import type { CSSProperties } from 'react'

interface AppIconProps {
  name: string
  size?: number
  color?: string
  style?: CSSProperties
}

export default function AppIcon({ name, size = 20, color, style }: AppIconProps) {
  return <Icon icon={name} width={size} height={size} color={color} style={style} />
}
