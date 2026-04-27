import { ExclamationCircleFilled } from '@ant-design/icons';
import { Modal, Typography, theme } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';

const { Text, Paragraph } = Typography;

/** localStorage 键，用于标记用户已同意免责声明。 */
const DISCLAIMER_AGREED_KEY = 'disclaimer-agreed';

/**
 * 检查用户是否已同意免责声明。
 *
 * @returns 是否已同意。
 */
function hasAgreed(): boolean {
  try {
    return localStorage.getItem(DISCLAIMER_AGREED_KEY) === '1';
  } catch {
    return false;
  }
}

/**
 * 持久化用户同意状态。
 */
function persistAgreement(): void {
  try {
    localStorage.setItem(DISCLAIMER_AGREED_KEY, '1');
  } catch {
    // Ignore write failures in restricted environments.
  }
}

/**
 * 免责声明弹窗组件。
 *
 * 登录后首次进入系统时展示，用户点击"我已知晓并同意"后将不再弹出。
 *
 * @returns 免责声明弹窗节点。
 */
const DisclaimerModal: React.FC = () => {
  const [open, setOpen] = useState(false);
  const { token } = theme.useToken();

  useEffect(() => {
    if (!hasAgreed()) {
      setOpen(true);
    }
  }, []);

  const handleAgree = useCallback(() => {
    persistAgreement();
    setOpen(false);
  }, []);

  return (
    <Modal
      open={open}
      closable={false}
      maskClosable={false}
      keyboard={false}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ExclamationCircleFilled style={{ color: token.colorWarning, fontSize: 20 }} />
          <span>免责声明</span>
        </div>
      }
      okText="我已知晓并同意"
      cancelButtonProps={{ style: { display: 'none' } }}
      onOk={handleAgree}
      width={520}
      centered
    >
      <div style={{ padding: '12px 0' }}>
        <Paragraph style={{ fontSize: 14, lineHeight: 1.8, marginBottom: 12 }}>
          本系统为<Text strong>个人面试项目展示</Text>用途，
          <Text strong>不对外提供任何真实的药品销售、医疗咨询或相关商业服务</Text>。
        </Paragraph>

        <Paragraph style={{ fontSize: 14, lineHeight: 1.8, marginBottom: 12 }}>
          系统中展示的所有药品数据均从公开网络渠道获取，仅用于功能演示，
          <Text strong>不代表真实药品信息，不具有任何医疗指导意义</Text>。
          如有侵权内容，请联系我及时删除。
        </Paragraph>

        <Paragraph style={{ fontSize: 14, lineHeight: 1.8, marginBottom: 12 }}>
          <Text type="danger" strong>
            严禁在本系统上发布任何违法违规信息。
          </Text>
          您在系统中的每一次操作都会被记录和审计，请规范使用。
        </Paragraph>

        <Paragraph style={{ fontSize: 14, lineHeight: 1.8, marginBottom: 12 }}>
          如需测试知识库上传、解析等功能，请勿上传任何包含个人隐私、商业资料、账号凭证、
          病历处方或其他敏感信息的文件。本系统为公网部署环境，
          <Text type="danger" strong>
            其他访问者可能看到相关内容，请谨防信息泄露。
          </Text>
        </Paragraph>

        <Paragraph
          style={{
            fontSize: 13,
            lineHeight: 1.6,
            marginBottom: 0,
            padding: '10px 12px',
            borderRadius: 8,
            background: token.colorFillAlter,
          }}
        >
          点击"我已知晓并同意"即表示您已阅读并理解以上声明内容。
        </Paragraph>
      </div>
    </Modal>
  );
};

export default DisclaimerModal;
