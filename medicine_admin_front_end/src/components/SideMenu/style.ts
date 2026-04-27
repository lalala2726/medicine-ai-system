import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token, css }) => {
  /** 侧边菜单背景色。 */
  const menuBackground = 'var(--layout-menu-bg, rgb(249, 250, 251))';
  /** 普通菜单文字颜色。 */
  const menuTextColor = 'var(--layout-menu-item-color, rgba(15, 23, 42, 0.8))';
  /** 菜单高亮文字颜色。 */
  const menuTextActiveColor = 'var(--layout-menu-item-active-color, rgba(15, 23, 42, 0.92))';
  /** 菜单图标默认颜色。 */
  const menuIconColor = 'var(--layout-menu-icon-color, rgba(15, 23, 42, 0.7))';
  /** 分组标题文字颜色。 */
  const groupTitleColor = 'var(--layout-menu-group-title-color, rgba(15, 23, 42, 0.46))';
  /** 分组标题悬停颜色。 */
  const groupTitleHoverColor = 'var(--layout-menu-group-title-hover-color, rgba(15, 23, 42, 0.58))';
  /** 菜单悬停背景色。 */
  const itemHoverBackground = 'var(--layout-menu-item-hover-bg, rgb(233, 233, 235))';
  /** 菜单选中背景色。 */
  const itemSelectedBackground = 'var(--layout-menu-item-selected-bg, rgb(233, 233, 235))';
  /** 展开箭头颜色。 */
  const expandArrowColor = 'var(--layout-menu-arrow-color, rgba(15, 23, 42, 0.28))';
  /** 菜单左右边距，保证与边缘留白一致。 */
  const menuItemInlineGap = 'var(--layout-menu-item-inline-gap, 12px)';
  /** 菜单项圆角。 */
  const menuItemRadius = 'var(--layout-menu-item-radius, 12px)';
  /** 菜单图标与文字之间的统一间距。 */
  const menuItemContentGap = 'var(--layout-menu-item-content-gap, 10px)';
  /** 单栏模式下右侧额外补偿，避免视觉上偏窄。 */
  const standaloneMenuRightOffset = 'var(--layout-menu-standalone-right-offset, 4px)';
  /** 菜单按钮基础样式。 */
  const buttonBase = css`
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    min-width: 0;
    border: none;
    outline: none;
    appearance: none;
    background: transparent;
    box-sizing: border-box;
    text-align: left;
    cursor: pointer;
    transition:
      background-color 0.2s ease,
      color 0.2s ease,
      transform 0.2s ease;
  `;

  return {
    sideMenuLayout: css`
      display: flex;
      height: 100%;
      overflow: hidden;
      background: ${menuBackground};
    `,

    primaryMenuColumn: css`
      flex: 0 0 auto;
      min-width: 0;
      height: 100%;
    `,

    sideMenuWrapper: css`
      height: 100%;
      overflow-y: auto;
      overflow-x: hidden;
      padding: 10px 0 12px;
      background: ${menuBackground};

      /* 隐藏滚动条但保留滚动能力。 */
      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background: transparent;
        border-radius: 4px;
      }

      &:hover::-webkit-scrollbar-thumb {
        background: ${token.colorTextQuaternary};
      }
    `,

    secondaryMenuDivider: css`
      flex: 0 0 1px;
      width: 1px;
      height: calc(100% - 24px);
      margin-top: 12px;
      background: var(--layout-divider-color, rgba(15, 23, 42, 0.08));
    `,

    secondaryMenuColumn: css`
      flex: 0 0 auto;
      min-width: 0;
      height: 100%;
      overflow-y: auto;
      overflow-x: hidden;
      background: ${menuBackground};

      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background: transparent;
        border-radius: 4px;
      }

      &:hover::-webkit-scrollbar-thumb {
        background: ${token.colorTextQuaternary};
      }
    `,

    menu: css`
      display: flex;
      flex-direction: column;
      padding: 0 ${menuItemInlineGap};
      box-sizing: border-box;
      background: ${menuBackground};
    `,

    menuCollapsed: css`
      padding-inline: 0;
    `,

    menuStandalone: css`
      padding-inline-end: calc(${menuItemInlineGap} + ${standaloneMenuRightOffset});
    `,

    menuGroup: css`
      display: flex;
      flex-direction: column;
    `,

    groupButton: css`
      ${buttonBase};
      gap: ${menuItemContentGap};
      min-height: 40px;
      margin: 7px 0 4px;
      padding: 0 38px 0 18px;
      border-radius: ${menuItemRadius};
      color: ${groupTitleColor};
      font-size: 13px;
      font-weight: 500;
      user-select: none;

      &:hover {
        color: ${groupTitleHoverColor};
      }
    `,

    groupButtonCollapsed: css`
      justify-content: center;
      width: 40px;
      min-height: 40px;
      margin: 2px auto;
      padding: 0;
      border-radius: ${menuItemRadius};
    `,

    groupButtonIcon: css`
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
      color: currentColor;
    `,

    groupLabelText: css`
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    `,

    submenuList: css`
      display: flex;
      flex-direction: column;
    `,

    submenuListOpen: css`
      display: flex;
    `,

    submenuListClosed: css`
      display: none;
    `,

    submenuListCollapsed: css`
      margin-bottom: 4px;
    `,

    menuItemButton: css`
      ${buttonBase};
      gap: ${menuItemContentGap};
      min-height: 42px;
      margin: 3px 0;
      padding: 0 18px;
      border-radius: ${menuItemRadius};
      color: ${menuTextColor};
      font-size: 14px;
      font-weight: 500;

      &[data-current='true'],
      &:hover {
        color: ${menuTextActiveColor};
      }

      &:hover {
        background: ${itemHoverBackground};
      }

      &:hover [data-side-menu-icon='true'] {
        color: ${menuTextActiveColor};
      }
    `,

    menuItemButtonSelected: css`
      background: ${itemSelectedBackground};
      color: ${menuTextActiveColor};

      [data-side-menu-icon='true'] {
        color: ${menuTextActiveColor};
      }
    `,

    menuItemButtonCollapsed: css`
      gap: 0;
      justify-content: center;
      width: 40px;
      min-height: 40px;
      margin: 2px auto;
      padding: 0;
      border-radius: ${menuItemRadius};
    `,

    menuItemButtonNested: css``,

    menuItemIcon: css`
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
      color: ${menuIconColor};
      transition: color 0.2s ease;
    `,

    menuItemIconNested: css`
      color: ${menuIconColor};
    `,

    menuItemLabel: css`
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    `,

    expandArrow: css`
      position: absolute;
      inset-inline-end: 16px;
      top: 50%;
      width: 8px;
      height: 8px;
      margin-top: -5px;
      color: ${expandArrowColor};
      border-right: 1.5px solid currentColor;
      border-bottom: 1.5px solid currentColor;
      transition:
        transform 0.2s ease,
        color 0.2s ease;
      pointer-events: none;
    `,

    expandArrowOpen: css`
      transform: rotate(45deg);
    `,

    expandArrowClosed: css`
      transform: rotate(-45deg);
    `,
  };
});

export default useStyles;
