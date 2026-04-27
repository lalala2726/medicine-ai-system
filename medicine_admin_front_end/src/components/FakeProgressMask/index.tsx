import { Progress } from 'antd';
import React from 'react';

export type FakeProgressStatus = 'normal' | 'success' | 'exception';

export interface FakeProgressMaskProps {
  visible: boolean;
  percent: number;
  text?: string;
  status?: FakeProgressStatus;
}

const FakeProgressMask: React.FC<FakeProgressMaskProps> = ({
  visible,
  percent,
  text = '正在识别中...',
  status = 'normal',
}) => {
  if (!visible) return null;

  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        background: 'rgba(255,255,255,0.75)',
        backdropFilter: 'blur(1px)',
        zIndex: 10,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        pointerEvents: 'auto',
      }}
    >
      <div
        style={{
          minWidth: 260,
          maxWidth: 420,
          padding: '16px 18px',
          background: '#fff',
          borderRadius: 8,
          boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <Progress
          percent={Math.min(100, Math.round(percent))}
          status={status === 'exception' ? 'exception' : percent >= 100 ? 'success' : 'active'}
          showInfo
        />
        <div style={{ textAlign: 'center', color: '#555', fontSize: 14 }}>{text}</div>
      </div>
    </div>
  );
};

export default FakeProgressMask;
