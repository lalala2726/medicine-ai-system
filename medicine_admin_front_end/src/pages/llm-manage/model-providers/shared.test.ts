import type { ModelProviderTypes } from '@/api/llm-manage/modelProviders';

import {
  buildProviderListRequest,
  buildProviderPayload,
  createAliyunInitialValues,
  mapPresetDetailToFormValues,
} from './shared';

describe('model provider shared helpers', () => {
  it('maps list request params to backend payload', () => {
    expect(
      buildProviderListRequest({
        current: 2,
        pageSize: 20,
        providerName: 'OpenAI',
      }),
    ).toEqual({
      pageNum: 2,
      pageSize: 20,
      providerName: 'OpenAI',
    });
  });

  it('creates Aliyun initial values', () => {
    expect(createAliyunInitialValues()).toEqual({
      providerKey: 'aliyun-bailian',
      providerName: '阿里云百联',
      providerType: 'aliyun',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      apiKey: '',
      description: '阿里云百联 OpenAI 兼容接口预设模板',
      sort: 0,
      models: [],
    });
  });

  it('maps preset detail to form values', () => {
    const detail: ModelProviderTypes.ProviderPresetDetail = {
      providerKey: 'aliyun-bailian',
      providerName: '阿里云百联',
      providerType: 'aliyun',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      description: 'preset',
      models: [
        {
          modelName: 'qwen-plus',
          modelType: 'CHAT',
          supportReasoning: 1,
          supportVision: 1,
          enabled: 0,
          sort: 10,
        },
      ],
    };

    expect(mapPresetDetailToFormValues(detail)).toEqual({
      providerKey: 'aliyun-bailian',
      providerName: '阿里云百联',
      providerType: 'aliyun',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      apiKey: '',
      description: 'preset',
      sort: 0,
      models: [
        {
          modelName: 'qwen-plus',
          modelType: 'CHAT',
          supportReasoning: 1,
          supportVision: 1,
          description: '',
          enabled: true,
        },
      ],
    });
  });

  it('builds create payload with normalized booleans and defaults', () => {
    const payload = buildProviderPayload(
      {
        providerKey: 'aliyun-bailian',
        providerName: ' 阿里云百联主配置 ',
        providerType: 'aliyun',
        baseUrl: ' https://dashscope.aliyuncs.com/compatible-mode/v1 ',
        apiKey: ' sk-xxx ',
        description: ' main ',
        sort: 10,
        models: [
          {
            modelName: ' qwen-plus ',
            modelType: 'CHAT',
            supportReasoning: 1,
            description: ' chat ',
            enabled: false,
          },
        ],
      },
      'create',
    );

    expect(payload).toEqual({
      providerKey: 'aliyun-bailian',
      providerName: '阿里云百联主配置',
      providerType: 'aliyun',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      apiKey: 'sk-xxx',
      description: 'main',
      sort: 10,
      models: [
        {
          modelName: 'qwen-plus',
          modelType: 'CHAT',
          supportReasoning: 1,
          supportVision: 0,
          description: 'chat',
          enabled: 1,
          sort: 0,
        },
      ],
    });
  });

  it('omits empty api key when building edit payload', () => {
    const payload = buildProviderPayload(
      {
        providerName: 'Custom Provider',
        providerType: 'aliyun',
        baseUrl: 'https://custom.example.com/v1',
        apiKey: '   ',
        description: '',
        sort: 0,
        models: [
          {
            modelName: 'text-embedding-3-small',
            modelType: 'EMBEDDING',
            enabled: true,
          },
        ],
      },
      'edit',
      '99',
    );

    expect(payload).toEqual({
      id: '99',
      providerKey: undefined,
      providerName: 'Custom Provider',
      providerType: 'aliyun',
      baseUrl: 'https://custom.example.com/v1',
      description: undefined,
      sort: 0,
      models: [
        {
          modelName: 'text-embedding-3-small',
          modelType: 'EMBEDDING',
          supportReasoning: 0,
          supportVision: 0,
          description: undefined,
          enabled: 0,
          sort: 0,
        },
      ],
    });
  });
});
