import { Descriptions, Image, Input, Space } from 'antd';
import React from 'react';
import type { MallAfterSaleTypes } from '@/api/mall/mallAfterSale';
import { getAfterSaleReasonText } from '@/types/afterSale';

const { TextArea } = Input;

interface AfterSaleDetailTabProps {
  detail: MallAfterSaleTypes.AfterSaleDetailVo;
  rejectReason: string;
  onRejectReasonChange: (value: string) => void;
  getAfterSaleTypeTag: (type?: string, typeName?: string) => React.ReactNode;
  getAfterSaleStatusTag: (status?: string, statusName?: string) => React.ReactNode;
}

const AfterSaleDetailTab: React.FC<AfterSaleDetailTabProps> = ({
  detail,
  rejectReason,
  onRejectReasonChange,
  getAfterSaleTypeTag,
  getAfterSaleStatusTag,
}) => {
  return (
    <div>
      <Descriptions column={1} bordered>
        <Descriptions.Item label="售后单号">{detail.afterSaleNo}</Descriptions.Item>
        <Descriptions.Item label="订单编号">{detail.orderNo}</Descriptions.Item>
        <Descriptions.Item label="售后类型">
          {getAfterSaleTypeTag(detail.afterSaleType, detail.afterSaleTypeName)}
        </Descriptions.Item>
        <Descriptions.Item label="售后状态">
          {getAfterSaleStatusTag(detail.afterSaleStatus, detail.afterSaleStatusName)}
        </Descriptions.Item>
        <Descriptions.Item label="退款金额">
          ¥{Number(detail.refundAmount || 0).toFixed(2)}
        </Descriptions.Item>
        <Descriptions.Item label="用户昵称">{detail.userNickname}</Descriptions.Item>
        <Descriptions.Item label="用户ID">{detail.userId}</Descriptions.Item>
        <Descriptions.Item label="申请原因">
          {detail.applyReasonName || getAfterSaleReasonText(detail.applyReason)}
        </Descriptions.Item>
        <Descriptions.Item label="详细说明">{detail.applyDescription || '-'}</Descriptions.Item>
        {detail.evidenceImages && detail.evidenceImages.length > 0 && (
          <Descriptions.Item label="凭证图片">
            <Space wrap>
              {detail.evidenceImages.map((img) => (
                <Image
                  key={img}
                  width={80}
                  height={80}
                  src={img}
                  alt="凭证图片"
                  style={{ objectFit: 'cover' }}
                />
              ))}
            </Space>
          </Descriptions.Item>
        )}
        {detail.rejectReason && (
          <Descriptions.Item label="拒绝原因">{detail.rejectReason}</Descriptions.Item>
        )}
        {detail.adminRemark && (
          <Descriptions.Item label="管理员备注">{detail.adminRemark}</Descriptions.Item>
        )}
        <Descriptions.Item label="申请时间">{detail.applyTime}</Descriptions.Item>
        {detail.auditTime && (
          <Descriptions.Item label="审核时间">{detail.auditTime}</Descriptions.Item>
        )}
        {detail.completeTime && (
          <Descriptions.Item label="完成时间">{detail.completeTime}</Descriptions.Item>
        )}
      </Descriptions>

      {/* 待审核状态显示拒绝原因输入框 */}
      {detail.afterSaleStatus === 'PENDING' && (
        <div style={{ marginTop: 24 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>拒绝原因：</div>
          <TextArea
            rows={4}
            placeholder="如果要拒绝该申请，请输入拒绝原因"
            value={rejectReason}
            onChange={(e) => onRejectReasonChange(e.target.value)}
            maxLength={200}
            showCount
          />
        </div>
      )}
    </div>
  );
};

export default AfterSaleDetailTab;
