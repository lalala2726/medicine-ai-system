import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  ApiOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Form, Input, Space, Spin, Steps, message } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
  addProvider,
  getProviderById,
  getProviderPresetDetail,
  getProviderPresetList,
  testProviderConnectivity,
  updateProvider,
  type ModelProviderTypes,
} from '@/api/llm-manage/modelProviders';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { routePaths } from '@/router/paths';

import ModelList from '../components/ModelList';
import ProviderBasicForm from '../components/ProviderBasicForm';
import {
  ALIYUN_BAILIAN_PROVIDER_KEY,
  buildProviderPayload,
  createAliyunInitialValues,
  mapPresetDetailToFormValues,
  mapProviderDetailToFormValues,
  PRESET_SOURCE,
  type ProviderFormValues,
} from '../shared';
import styles from './index.module.less';

const ModelProviderEditor: React.FC = () => {
  const [form] = Form.useForm<ProviderFormValues>();
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const [searchParams] = useSearchParams();
  const [messageApi, contextHolder] = message.useMessage();

  const [detailLoading, setDetailLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [testingConnectivity, setTestingConnectivity] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);

  const isEdit = Boolean(id);
  const source = searchParams.get('source') || PRESET_SOURCE;
  const providerKey = searchParams.get('providerKey') || ALIYUN_BAILIAN_PROVIDER_KEY;

  const loadPageData = useCallback(async () => {
    setDetailLoading(true);
    try {
      const presetRequest = getProviderPresetList().catch((error) => {
        console.error('加载预设提供商失败:', error);
        return [];
      });

      if (isEdit && id) {
        const [, detail] = await Promise.all([presetRequest, getProviderById(id)]);
        form.setFieldsValue(mapProviderDetailToFormValues(detail));
        return;
      }

      if (source === PRESET_SOURCE) {
        const [, detail] = await Promise.all([presetRequest, getProviderPresetDetail(providerKey)]);
        form.setFieldsValue(mapPresetDetailToFormValues(detail));
        return;
      }
      await presetRequest;
      form.setFieldsValue(createAliyunInitialValues());
    } catch (error) {
      console.error('加载模型提供商页面失败:', error);
      messageApi.error('加载模型提供商配置失败');
      navigate(routePaths.llmModelProviders);
    } finally {
      setDetailLoading(false);
    }
  }, [form, id, isEdit, messageApi, navigate, providerKey, source]);

  useEffect(() => {
    void loadPageData();
  }, [loadPageData]);

  const handleBack = useCallback(() => {
    navigate(routePaths.llmModelProviders);
  }, [navigate]);

  const handleNext = async () => {
    try {
      await form.validateFields(['providerName', 'providerType', 'apiKey', 'baseUrl']);
      setCurrentStep(1);
    } catch {
      messageApi.warning('请完善基础信息后再继续');
    }
  };

  const handlePrev = () => {
    setCurrentStep(0);
  };

  const handleTestConnectivity = async () => {
    try {
      const values = await form.validateFields(['baseUrl', 'apiKey']);
      if (!values.baseUrl || !values.apiKey) {
        messageApi.warning('请填写 Base URL 和 API Key');
        return;
      }
      setTestingConnectivity(true);
      const res = await testProviderConnectivity({
        baseUrl: values.baseUrl,
        apiKey: values.apiKey,
      });

      if (res.success) {
        messageApi.success({
          content: `连通成功 (耗时: ${res.latencyMs}ms)`,
          duration: 3,
        });
      } else {
        messageApi.error({
          content: `${res.message} (耗时: ${res.latencyMs}ms)`,
          duration: 4,
        });
      }
    } catch (error: any) {
      if (error?.errorFields) {
        messageApi.warning('请完善 Base URL 和 API Key');
        return;
      }
      messageApi.error('连通性测试请求失败');
    } finally {
      setTestingConnectivity(false);
    }
  };

  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      if (!values.models?.length) {
        messageApi.warning('请至少添加一个模型');
        return;
      }

      setSubmitting(true);
      const payload = buildProviderPayload(values, isEdit ? 'edit' : 'create', id);

      if (isEdit) {
        await updateProvider(payload as ModelProviderTypes.ProviderUpdateRequest);
        messageApi.success('保存成功');
      } else {
        await addProvider(payload as ModelProviderTypes.ProviderCreateRequest);
        messageApi.success('创建成功');
      }

      navigate(routePaths.llmModelProviders);
    } catch (error: any) {
      if (error?.errorFields) {
        messageApi.warning('请完善表单信息');
        return;
      }
      console.error('保存模型提供商失败:', error.message || '未知错误');
      messageApi.error(isEdit ? '保存失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  }, [form, id, isEdit, messageApi, navigate]);

  const pageTitle = isEdit ? '编辑模型提供商' : '新增模型提供商';
  const helperText = '当前仅支持阿里云百联预设接入，您可以按实际情况调整 API 参数与模型映射。';

  return (
    <PageContainer title={pageTitle} onBack={handleBack} fixedHeader>
      {contextHolder}
      <div className={styles.page}>
        <div className={styles.stepsWrapper}>
          <Steps
            current={currentStep}
            items={[
              { title: '基础信息', icon: <ApiOutlined /> },
              { title: '模型列表', icon: <SettingOutlined /> },
            ]}
          />
        </div>
        <Spin spinning={detailLoading}>
          <Form
            form={form}
            layout="vertical"
            initialValues={createAliyunInitialValues()}
            autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
          >
            <Form.Item name="providerKey" hidden>
              <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} />
            </Form.Item>
            <Form.Item name="models" hidden>
              <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} />
            </Form.Item>
            <div style={{ display: currentStep === 0 ? 'block' : 'none' }}>
              <ProviderBasicForm isEdit={isEdit} helperText={helperText} />
            </div>
            <div style={{ display: currentStep === 1 ? 'block' : 'none' }}>
              <ModelList form={form} />
            </div>
          </Form>
        </Spin>
      </div>
      <div className={styles.footer}>
        <div className={styles.footerContent}>
          <Space size="middle">
            {currentStep === 0 ? (
              <>
                <Button onClick={handleBack}>取消</Button>
                {!isEdit && (
                  <PermissionButton
                    access={ADMIN_PERMISSIONS.llmProvider.test}
                    onClick={handleTestConnectivity}
                    loading={testingConnectivity}
                  >
                    测试连通性
                  </PermissionButton>
                )}
                <Button
                  type="primary"
                  onClick={handleNext}
                  icon={<ArrowRightOutlined />}
                  iconPosition="end"
                >
                  下一步
                </Button>
              </>
            ) : (
              <>
                <Button onClick={handlePrev} icon={<ArrowLeftOutlined />}>
                  上一步
                </Button>
                <PermissionButton
                  type="primary"
                  loading={submitting}
                  access={
                    isEdit
                      ? ADMIN_PERMISSIONS.llmProvider.update
                      : ADMIN_PERMISSIONS.llmProvider.add
                  }
                  onClick={handleSave}
                  icon={<CheckOutlined />}
                  style={{ background: '#52c41a', borderColor: '#52c41a' }}
                >
                  {isEdit ? '确认保存' : '完成并创建'}
                </PermissionButton>
              </>
            )}
          </Space>
        </div>
      </div>
    </PageContainer>
  );
};

export default ModelProviderEditor;
