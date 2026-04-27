export const formatCurrency = (value?: number | string) =>
  `¥${(Number(value) || 0).toLocaleString()}`;
export const formatNumber = (value?: number | string) => (Number(value) || 0).toLocaleString();
export const formatPercent = (value?: number | string) =>
  `${((Number(value) || 0) * 100).toFixed(1)}%`;
export const formatHours = (value?: number | string) => `${(Number(value) || 0).toFixed(1)}h`;
