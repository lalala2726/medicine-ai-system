import React from 'react';
import styles from './index.module.less';

interface ChartLoaderProps {
  height?: number;
  containerClassName?: string;
}

/**
 * 图表加载动画组件
 * 用于所有图表的加载状态显示
 */
const ChartLoader: React.FC<ChartLoaderProps> = ({ height = 500, containerClassName }) => {
  return (
    <div
      style={{
        width: '100%',
        height,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      }}
      className={containerClassName}
    >
      <div className={styles.loader}></div>
    </div>
  );
};

export default ChartLoader;
