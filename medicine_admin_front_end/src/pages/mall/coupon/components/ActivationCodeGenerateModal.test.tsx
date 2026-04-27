import React from 'react';
import dayjs from 'dayjs';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import ActivationCodeGenerateModal from './ActivationCodeGenerateModal';
import {
  buildActivationCodeGeneratePayload,
  type ActivationCodeGenerateFormValues,
} from './activationCodeGenerateHelper';

jest.mock('@/api/mall/coupon', () => ({
  generateActivationCodes: jest.fn(),
}));

import { generateActivationCodes } from '@/api/mall/coupon';

jest.mock('./CouponTemplateSelector', () => {
  return function MockCouponTemplateSelector(props: {
    value?: number;
    onChange?: (value?: number) => void;
  }) {
    return (
      <input
        data-testid="template-selector"
        value={props.value ?? ''}
        onChange={(event) => props.onChange?.(Number(event.target.value))}
      />
    );
  };
});

describe('ActivationCodeGenerateModal', () => {
  /**
   * 验证生成参数映射会按模式与有效期类型输出正确请求体。
   */
  it('buildActivationCodeGeneratePayload_ShouldMapFormValues', () => {
    const values: ActivationCodeGenerateFormValues = {
      templateId: 1,
      redeemRuleType: 'UNIQUE_SINGLE_USE',
      generateCount: 20,
      validityType: 'AFTER_ACTIVATION',
      relativeValidDays: 15,
      remark: '四月活动 ',
    };

    expect(buildActivationCodeGeneratePayload(values)).toEqual({
      templateId: 1,
      redeemRuleType: 'UNIQUE_SINGLE_USE',
      generateCount: 20,
      validityType: 'AFTER_ACTIVATION',
      fixedEffectiveTime: undefined,
      fixedExpireTime: undefined,
      relativeValidDays: 15,
      remark: '四月活动',
    });

    expect(
      buildActivationCodeGeneratePayload({
        templateId: 2,
        redeemRuleType: 'SHARED_PER_USER_ONCE',
        generateCount: 8,
        validityType: 'ONCE',
        fixedRange: [dayjs('2026-04-09T12:00:00+08:00'), dayjs('2026-05-09T12:00:00+08:00')],
      }),
    ).toMatchObject({
      templateId: 2,
      redeemRuleType: 'SHARED_PER_USER_ONCE',
      generateCount: 1,
      validityType: 'ONCE',
      fixedEffectiveTime: '2026-04-09T12:00:00.000+08:00',
      fixedExpireTime: '2026-05-09T12:00:00.000+08:00',
    });
  });

  /**
   * 验证模式与有效期切换时会展示对应表单项。
   */
  it('render_ShouldToggleModeAndValidityFields', () => {
    render(<ActivationCodeGenerateModal open onOpenChange={jest.fn()} onGenerated={jest.fn()} />);

    expect(screen.queryByText('生成数量')).toBeNull();
    expect(screen.getByText('固定生效区间')).toBeTruthy();
    expect(screen.queryByText('激活后有效天数')).toBeNull();

    fireEvent.click(screen.getByLabelText('唯一码'));
    fireEvent.click(screen.getByLabelText('激活后计算'));

    expect(screen.getByText('生成数量')).toBeTruthy();
    expect(screen.queryByText('固定生效区间')).toBeNull();
    expect(screen.getByText('激活后有效天数')).toBeTruthy();
  });

  /**
   * 验证生成成功后抽屉会切换到结果态，并逐行展示激活码。
   */
  it('submit_ShouldSwitchToResultView', async () => {
    (generateActivationCodes as jest.Mock).mockResolvedValue({
      batchNo: 'ACT202604091200000001',
      templateName: '新人100元券',
      redeemRuleType: 'UNIQUE_SINGLE_USE',
      validityType: 'AFTER_ACTIVATION',
      generatedCount: 2,
      codes: [
        { id: 1, plainCode: 'ABCD1234EFGH5678' },
        { id: 2, plainCode: 'IJKL1234MNOP5678' },
      ],
    });

    render(<ActivationCodeGenerateModal open onOpenChange={jest.fn()} onGenerated={jest.fn()} />);

    fireEvent.change(screen.getByTestId('template-selector'), {
      target: { value: '1' },
    });
    fireEvent.click(screen.getByLabelText('唯一码'));
    fireEvent.click(screen.getByLabelText('激活后计算'));

    const spinButtons = screen.getAllByRole('spinbutton');
    fireEvent.change(spinButtons[0], { target: { value: '20' } });
    fireEvent.change(spinButtons[1], { target: { value: '15' } });

    fireEvent.click(screen.getByRole('button', { name: /生\s*成/ }));

    await waitFor(() => {
      expect(screen.getByText('激活码明文仅在当前抽屉展示，请及时复制或导出保存。')).toBeTruthy();
      expect(screen.getByText('ABCD1234EFGH5678')).toBeTruthy();
      expect(screen.getByText('IJKL1234MNOP5678')).toBeTruthy();
    });
  });
});
