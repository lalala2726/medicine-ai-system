import { requestClient } from '@/utils/request';

export namespace AnalyticsTypes {
  export type DaysRange = 7 | 15 | 30 | 84 | 365;

  export interface AnalyticsRealtimeOverviewVo {
    cumulativePaidAmount?: string;
    cumulativePaidOrderCount?: string;
    todayPaidAmount?: string;
    todayPaidOrderCount?: string;
    pendingShipmentOrderCount?: string;
    pendingReceiptOrderCount?: string;
    pendingAfterSaleCount?: string;
    processingAfterSaleCount?: string;
  }

  export interface AnalyticsRangeSummaryVo {
    paidAmount?: string;
    paidOrderCount?: string;
    averageOrderAmount?: string;
    refundAmount?: string;
    netPaidAmount?: string;
    refundRate?: string;
    afterSaleApplyCount?: string;
    returnRefundQuantity?: string;
  }

  export interface AnalyticsConversionSummaryVo {
    createdOrderCount?: string;
    paidOrderCount?: string;
    pendingPaymentOrderCount?: string;
    closedOrderCount?: string;
    paymentConversionRate?: string;
  }

  export interface AnalyticsFulfillmentSummaryVo {
    averageShipmentHours?: string;
    averageReceiptHours?: string;
    overdueShipmentOrderCount?: string;
    overdueReceiptOrderCount?: string;
  }

  export interface AnalyticsAfterSaleEfficiencySummaryVo {
    averageAuditHours?: string;
    averageCompleteHours?: string;
    overdueAuditCount?: string;
    overdueCompleteCount?: string;
  }

  export interface AnalyticsStatusDistributionItemVo {
    status?: string;
    statusName?: string;
    count?: string;
  }

  export interface AnalyticsReasonDistributionItemVo {
    reason?: string;
    reasonName?: string;
    count?: string;
  }

  export interface AnalyticsTopSellingProductVo {
    productId?: string;
    productName?: string;
    productImage?: string;
    soldQuantity?: string;
    paidAmount?: string;
  }

  export interface AnalyticsReturnRefundRiskProductVo {
    productId?: string;
    productName?: string;
    productImage?: string;
    soldQuantity?: string;
    returnRefundQuantity?: string;
    returnRefundRate?: string;
    refundAmount?: string;
  }

  export interface AnalyticsDashboardVo {
    realtimeOverview?: AnalyticsRealtimeOverviewVo;
    rangeSummary?: AnalyticsRangeSummaryVo;
    conversionSummary?: AnalyticsConversionSummaryVo;
    fulfillmentSummary?: AnalyticsFulfillmentSummaryVo;
    afterSaleEfficiencySummary?: AnalyticsAfterSaleEfficiencySummaryVo;
    afterSaleStatusDistribution?: AnalyticsStatusDistributionItemVo[];
    afterSaleReasonDistribution?: AnalyticsReasonDistributionItemVo[];
    topSellingProducts?: AnalyticsTopSellingProductVo[];
    returnRefundRiskProducts?: AnalyticsReturnRefundRiskProductVo[];
  }

  export interface AnalyticsTrendPointVo {
    label?: string;
    paidOrderCount?: string;
    paidAmount?: string;
    refundAmount?: string;
    afterSaleApplyCount?: string;
  }

  export interface AnalyticsTrendVo {
    days?: number;
    granularity?: 'DAY' | 'WEEK' | 'MONTH';
    points?: AnalyticsTrendPointVo[];
  }
}

export async function getAnalyticsDashboard(
  params: {
    days?: number;
  },
  options?: { [key: string]: any },
) {
  return requestClient.get<AnalyticsTypes.AnalyticsDashboardVo>('/mall/analytics/dashboard', {
    params,
    ...options,
  });
}

export async function getAnalyticsTrend(
  params: {
    days?: number;
  },
  options?: { [key: string]: any },
) {
  return requestClient.get<AnalyticsTypes.AnalyticsTrendVo>('/mall/analytics/trend', {
    params,
    ...options,
  });
}
