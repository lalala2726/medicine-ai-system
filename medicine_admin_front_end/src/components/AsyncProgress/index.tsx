import { Flex, Progress } from 'antd';
import React, { useEffect, useMemo, useRef, useState } from 'react';

export type AsyncProgressState = 'idle' | 'running' | 'success' | 'error';

export interface AsyncProgressProps {
  state: AsyncProgressState;
  /** 当 progress 状态为 success 时是否保留 100%，否则一律重置 */
  keepOnSuccess?: boolean;
}

/**
 * 异步进度条：
 * - running 时自动平滑前进到 90%+，不达 100%
 * - success 时瞬间到 100%
 * - error 时标红显示异常
 * - idle 时重置为 0
 */
const AsyncProgress: React.FC<AsyncProgressProps> = ({ state, keepOnSuccess = true }) => {
  const [percent, setPercent] = useState(0);
  const timerRef = useRef<NodeJS.Timeout | undefined>(undefined);

  const status = useMemo(() => {
    if (state === 'error') return 'exception' as const;
    if (state === 'success') return 'success' as const;
    return 'active' as const;
  }, [state]);

  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }

    if (state === 'idle') {
      setPercent(0);
      return;
    }

    if (state === 'running') {
      setPercent((prev) => (prev > 10 ? prev : 10));
      timerRef.current = setInterval(() => {
        setPercent((prev) => {
          const next = prev + Math.random() * 6 + 2; // 平滑递增
          return next >= 96 ? 96 : next;
        });
      }, 500);
      return () => {
        if (timerRef.current) {
          clearInterval(timerRef.current);
        }
      };
    }

    if (state === 'success') {
      setPercent(100);
      return undefined;
    }

    if (state === 'error') {
      setPercent((prev) => (prev > 30 ? prev : 30));
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [state]);

  // 成功后如果不需要保留，则短暂延迟重置
  useEffect(() => {
    if (state === 'success' && !keepOnSuccess) {
      const t = setTimeout(() => setPercent(0), 800);
      return () => clearTimeout(t);
    }
    return undefined;
  }, [state, keepOnSuccess]);

  if (state === 'idle') {
    return null;
  }

  return (
    <Flex gap="small" vertical>
      <Progress percent={Math.round(percent)} status={status} />
    </Flex>
  );
};

export default AsyncProgress;
