import React, { useState, useEffect, useRef, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Button, DatePicker, Avatar, Input } from '@nutui/nutui-react'
import { ChevronLeft, ChevronRight, User, Camera } from 'lucide-react'
import { useUserStore } from '@/stores/userStore'
import { showSuccessNotify } from '@/utils/notify'
import { updateUserProfile, type UserTypes } from '@/api/user'
import { uploadFile } from '@/api/common'
import { useAvatarTransitionStore } from '@/stores/avatarTransitionStore'
import AvatarTransition from '@/components/AvatarTransition'
import styles from './index.module.less'

/**
 * 个人资料编辑表单数据。
 */
interface ProfileEditFormData {
  /** 头像 */
  avatar: string
  /** 昵称 */
  nickname: string
  /** 真实姓名 */
  realName: string
  /** 生日 */
  birthday: string
}

/**
 * 个人资料编辑页面。
 *
 * @returns 页面节点
 */
const ProfileEdit: React.FC = () => {
  const navigate = useNavigate()
  const { user, profile } = useUserStore()
  const [form] = Form.useForm()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const targetAvatarRef = useRef<HTMLDivElement>(null)
  const { isAnimating, setSourceRect, startAnimation } = useAvatarTransitionStore()

  /**
   * 表单初始数据，用于变化检测。
   */
  const initialData = useMemo<ProfileEditFormData>(
    () => ({
      nickname: profile.nickname || '',
      realName: profile.realName || '',
      birthday: profile.birthday || '',
      avatar: profile.avatar || user.avatar || ''
    }),
    [profile, user.avatar]
  )

  const [formData, setFormData] = useState<ProfileEditFormData>(initialData)
  const [showDatePicker, setShowDatePicker] = useState(false)

  /**
   * 判断表单是否已修改。
   */
  const hasChanges = useMemo(() => {
    return (
      formData.nickname !== initialData.nickname ||
      formData.realName !== initialData.realName ||
      formData.birthday !== initialData.birthday ||
      formData.avatar !== initialData.avatar
    )
  }, [formData, initialData])

  useEffect(() => {
    form.setFieldsValue(formData)
  }, [formData, form])

  /**
   * 返回上一页。
   *
   * @returns 无返回值
   */
  const handleBack = () => {
    if (targetAvatarRef.current) {
      const rect = targetAvatarRef.current.getBoundingClientRect()
      setSourceRect(
        { top: rect.top, left: rect.left, width: rect.width, height: rect.height },
        formData.avatar || user.avatar,
        'backward'
      )
      startAnimation()
    }
    navigate(-1)
  }

  /**
   * 保存个人资料。
   *
   * @returns 无返回值
   */
  const onSave = async () => {
    if (!hasChanges) return
    try {
      const profileUpdatePayload: UserTypes.UserProfileDto = {
        avatar: formData.avatar,
        nickname: formData.nickname,
        realName: formData.realName,
        birthday: formData.birthday
      }
      await updateUserProfile(profileUpdatePayload)
      const { updateFromProfile } = useUserStore.getState()
      updateFromProfile(profileUpdatePayload)
      showSuccessNotify('个人资料已更新')
      setTimeout(() => {
        navigate(-1)
      }, 800)
    } catch (errors) {
      console.error('保存失败:', errors)
    }
  }

  /**
   * 确认出生日期。
   *
   * @param _options 选择项集合
   * @param values 选中的日期值
   * @returns 无返回值
   */
  const confirmDate = (_options: any[], values: (string | number | null)[]) => {
    const dateStr = values.filter(v => v !== null).join('-')
    const updated = { ...formData, birthday: dateStr }
    setFormData(updated)
    form.setFieldsValue(updated)
    setShowDatePicker(false)
  }

  /**
   * 打开头像选择器。
   *
   * @returns 无返回值
   */
  const activeAvatar = () => {
    fileInputRef.current?.click()
  }

  /**
   * 处理头像文件选择。
   *
   * @param event 文件选择事件
   * @returns 无返回值
   */
  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      try {
        const res = await uploadFile(file)
        if (res.fileUrl) {
          setFormData({ ...formData, avatar: res.fileUrl })
        }
      } catch (error) {
        console.error('上传头像失败', error)
      }
    }
  }

  /**
   * 更新表单字段。
   *
   * @param field 字段名
   * @param value 字段值
   * @returns 无返回值
   */
  const handleInputChange = (field: keyof ProfileEditFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  return (
    <div className={styles.profileEditPage}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} color='#0d1b12' />
        </div>
        <div className={styles.navTitle}>个人中心</div>
        <div className={styles.navRight}></div>
      </div>

      <div className={styles.contentWrapper}>
        <div className={styles.card}>
          {/* Avatar Row */}
          <div className={`${styles.formItem} ${styles.avatarItem}`} onClick={activeAvatar}>
            <div className={styles.label}>头像</div>
            <div className={styles.valueWrapper}>
              <div ref={targetAvatarRef} style={{ opacity: isAnimating ? 0 : 1, position: 'relative' }}>
                <Avatar
                  size='large'
                  src={formData.avatar || user.avatar}
                  icon={<User />}
                  className={styles.smallAvatar}
                />
                <div
                  style={{
                    position: 'absolute',
                    right: -2,
                    bottom: -2,
                    background: 'var(--nutui-color-primary)',
                    borderRadius: '50%',
                    padding: '4px',
                    border: '2px solid #fff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff'
                  }}
                >
                  <Camera size={10} fill='currentColor' />
                </div>
              </div>
              <ChevronRight size={16} color='#ccc' style={{ marginLeft: 4 }} />
              <input
                type='file'
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept='image/*'
                onChange={handleFileChange}
              />
            </div>
          </div>
          <AvatarTransition targetRef={targetAvatarRef} />

          <Form form={form} initialValues={formData} className={styles.customForm}>
            <div className={styles.formItem}>
              <div className={styles.label}>昵称</div>
              <div className={styles.inputWrapper}>
                <Input
                  className={styles.customInput}
                  value={formData.nickname}
                  onChange={val => handleInputChange('nickname', val)}
                  placeholder='设置您的昵称'
                  align='right'
                />
              </div>
            </div>

            <div className={styles.formItem}>
              <div className={styles.label}>真实姓名</div>
              <div className={styles.inputWrapper}>
                <Input
                  className={styles.customInput}
                  value={formData.realName}
                  onChange={val => handleInputChange('realName', val)}
                  placeholder='请完善真实姓名'
                  align='right'
                />
              </div>
            </div>

            <div className={styles.formItem} onClick={() => setShowDatePicker(true)}>
              <div className={styles.label}>出生年月</div>
              <div className={styles.valueWrapper}>
                <div className={`${styles.valueText} ${!formData.birthday ? styles.placeholder : ''}`}>
                  {formData.birthday || '选择日期'}
                </div>
                <ChevronRight size={16} color='#ccc' />
              </div>
            </div>
          </Form>
        </div>
      </div>

      <div className={styles.footer}>
        <Button block type='primary' fill='solid' className={styles.saveBtn} disabled={!hasChanges} onClick={onSave}>
          更新资料
        </Button>
      </div>

      <DatePicker
        title='选择出生日期'
        visible={showDatePicker}
        type='date'
        startDate={new Date(1900, 0, 1)}
        endDate={new Date()}
        value={formData.birthday ? new Date(formData.birthday) : new Date()}
        onConfirm={confirmDate}
        onClose={() => setShowDatePicker(false)}
      />
    </div>
  )
}

export default ProfileEdit
