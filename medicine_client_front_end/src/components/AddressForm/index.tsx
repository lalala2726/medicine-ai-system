import React, { useState, useEffect, useRef } from 'react'
import { Popup, Form, Input, Button, Switch, Cascader } from '@nutui/nutui-react'
import type { CascaderOption } from '@nutui/nutui-react'
import { ChevronRight } from 'lucide-react'
import { getChildren } from '@/api/regionAddress'
import { addAddress, updateAddress, type UserAddressTypes } from '@/api/userAddress'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

interface AddressFormProps {
  visible: boolean
  editData?: UserAddressTypes.UserAddressVo | null
  onClose: () => void
  onSuccess: () => void
}

const REGION_LEVEL_ORDER = [1, 2, 3, 4, 5] as const
type RegionLevel = (typeof REGION_LEVEL_ORDER)[number]

const AddressForm: React.FC<AddressFormProps> = ({ visible, editData, onClose, onSuccess }) => {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [cascaderVisible, setCascaderVisible] = useState(false)
  const [cascaderOptions, setCascaderOptions] = useState<CascaderOption[]>([])
  const [regionText, setRegionText] = useState('')
  // 使用 ref 保存当前选择的路径，确保在异步关闭时能获取到最新值
  const currentPathRef = useRef<CascaderOption[]>([])
  const regionLevelTextRef = useRef<Partial<Record<RegionLevel, string>>>({})

  const resetRegionLevels = () => {
    regionLevelTextRef.current = {}
  }

  const buildRegionTextFromLevels = () =>
    REGION_LEVEL_ORDER.map(level => regionLevelTextRef.current[level])
      .filter(Boolean)
      .join(' ')

  const syncRegionLevels = (path: CascaderOption[]) => {
    if (!path || path.length === 0) {
      resetRegionLevels()
      setRegionText('')
      return
    }

    let deepestLevel = 0

    path.forEach(option => {
      const optionLevel = typeof option.level === 'number' ? option.level : undefined
      const optionText = typeof option.text === 'string' ? option.text : ''

      if (optionLevel && REGION_LEVEL_ORDER.includes(optionLevel as RegionLevel) && optionLevel > deepestLevel) {
        deepestLevel = optionLevel
      }

      if (optionLevel && REGION_LEVEL_ORDER.includes(optionLevel as RegionLevel) && optionText) {
        regionLevelTextRef.current[optionLevel as RegionLevel] = optionText
      }
    })

    if (deepestLevel > 0) {
      REGION_LEVEL_ORDER.forEach(level => {
        if (level > deepestLevel) {
          delete regionLevelTextRef.current[level]
        }
      })
    }

    setRegionText(buildRegionTextFromLevels())
  }

  // 初始化表单数据
  useEffect(() => {
    if (visible) {
      if (editData) {
        // 编辑模式
        form.setFieldsValue({
          receiverName: editData.receiverName || '',
          receiverPhone: editData.receiverPhone || '',
          detailAddress: editData.detailAddress || '',
          isDefault: editData.isDefault === 1
        })
        // 设置地区显示文本
        if (editData.address) {
          setRegionText(editData.address)
        }
      } else {
        // 新增模式
        form.resetFields()
        setRegionText('')
        resetRegionLevels()
      }
    }
  }, [visible, editData, form])

  // 加载省级数据
  useEffect(() => {
    if (visible && cascaderVisible && cascaderOptions.length === 0) {
      loadProvinces()
    }
  }, [visible, cascaderVisible, cascaderOptions.length])

  const loadProvinces = async () => {
    try {
      // parentId 为 "0" 时获取所有省份
      const provinces = await getChildren('0')
      const options: CascaderOption[] = provinces.map(province => ({
        value: province.id!,
        text: province.name!,
        children: [],
        level: province.level
      }))
      setCascaderOptions(options)
    } catch (error) {
      console.error('加载省份失败:', error)
      showNotify('加载省份失败')
    }
  }

  // 懒加载子级数据 - 返回 Promise
  const lazyLoad = async (node: any): Promise<CascaderOption[]> => {
    try {
      const children = await getChildren(node.value as string)

      if (!children || children.length === 0) {
        setCascaderVisible(false)
        return []
      }

      const options: CascaderOption[] = children.map(item => {
        const option: CascaderOption = {
          value: item.id!,
          text: item.name!,
          children: [],
          level: item.level
        }
        return option
      })

      return options
    } catch (error) {
      console.error('加载地区数据失败:', error)
      showNotify('加载地区数据失败')
      return []
    }
  }

  // 选择地区
  const handleRegionChange = (_value: (string | number)[], path: CascaderOption[]) => {
    currentPathRef.current = path
    syncRegionLevels(path)
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      await form.validateFields()
      const values = form.getFieldsValue(true)

      if (!regionText) {
        showNotify('请选择所在地区')
        return
      }

      const phoneReg = /^1[3-9]\d{9}$/
      if (!phoneReg.test(values.receiverPhone)) {
        showNotify('请输入正确的手机号')
        return
      }

      setSubmitting(true)

      const requestData: UserAddressTypes.UserAddressRequest = {
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        address: regionText,
        detailAddress: values.detailAddress,
        isDefault: values.isDefault ? 1 : 0
      }

      if (editData?.id) {
        requestData.id = editData.id
        await updateAddress(requestData)
        showSuccessNotify('修改成功')
      } else {
        await addAddress(requestData)
        showSuccessNotify('添加成功')
      }

      onSuccess()
      onClose()
    } catch (error) {
      console.error('保存地址失败:', error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Popup visible={visible} position='bottom' round closeable onClose={onClose} style={{ height: '70vh' }}>
        <div className={styles.addressForm}>
          <div className={styles.formHeader}>
            <h3 className={styles.formTitle}>{editData ? '编辑收货地址' : '新增收货地址'}</h3>
          </div>
          <div className={styles.formContent}>
            <Form form={form} labelPosition='left'>
              <Form.Item label='收货人' name='receiverName' rules={[{ required: true, message: '请输入收货人姓名' }]}>
                <Input placeholder='请填写姓名' />
              </Form.Item>
              <Form.Item label='手机号' name='receiverPhone' rules={[{ required: true, message: '请输入手机号' }]}>
                <Input placeholder='请填写手机号' type='tel' maxLength={11} />
              </Form.Item>
              <Form.Item label='所在地区' required>
                <div className={styles.regionSelector} onClick={() => setCascaderVisible(true)}>
                  <span className={regionText ? styles.regionText : styles.regionPlaceholder}>
                    {regionText || '省、市、区、街道等'}
                  </span>
                  <ChevronRight size={16} color='#ccc' />
                </div>
              </Form.Item>
              <Form.Item label='详细地址' name='detailAddress' rules={[{ required: true, message: '请输入详细地址' }]}>
                <Input placeholder='街道、楼牌号等' maxLength={100} />
              </Form.Item>
              <Form.Item label='设为默认' name='isDefault' initialValue={false}>
                <div style={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
                  <Switch activeText='' inactiveText='' />
                </div>
              </Form.Item>
            </Form>
          </div>
          <div className={styles.formFooter}>
            <Button block type='primary' onClick={handleSubmit} loading={submitting}>
              保存地址
            </Button>
          </div>
        </div>
      </Popup>

      <Cascader
        visible={cascaderVisible}
        title='选择地区'
        options={cascaderOptions}
        onClose={() => setCascaderVisible(false)}
        onChange={handleRegionChange}
        onPathChange={handleRegionChange}
        lazy
        onLoad={lazyLoad}
      />
    </>
  )
}

export default AddressForm
