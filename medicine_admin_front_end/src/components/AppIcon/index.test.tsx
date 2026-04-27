import React from 'react';
import { fireEvent, render } from '@testing-library/react';
import AppIcon from '.';

describe('AppIcon', () => {
  it('renders public svg icons by name', () => {
    const { container } = render(<AppIcon name="provider" size={20} className="provider-icon" />);

    const icon = container.querySelector('[data-icon="provider"]') as HTMLSpanElement | null;

    expect(icon).not.toBeNull();
    expect(icon?.tagName).toBe('SPAN');
    expect(icon?.getAttribute('class')).toContain('provider-icon');
    expect(icon?.style.width).toBe('20px');
    expect(icon?.style.height).toBe('20px');
    expect(icon?.style.backgroundColor).toBe('currentColor');
    expect(icon?.style.maskImage).toContain('/icons/provider.svg');
  });

  it('applies the provider default size override', () => {
    const { container } = render(<AppIcon name="provider" />);

    const icon = container.querySelector('[data-icon="provider"]') as HTMLSpanElement | null;

    expect(icon?.style.width).toBe('17px');
    expect(icon?.style.height).toBe('17px');
  });

  it('applies the llms-settings visual size override', () => {
    const { container } = render(<AppIcon name="llms-settings" />);

    const icon = container.querySelector('[data-icon="llms-settings"]') as HTMLSpanElement | null;

    expect(icon?.style.width).toBe('19px');
    expect(icon?.style.height).toBe('19px');
  });

  it('renders system icons with ant design components', () => {
    const { container } = render(<AppIcon name="setting" className="setting-icon" size={18} />);

    expect(container.querySelector('.setting-icon')).not.toBeNull();
    expect(container.querySelector('[data-icon="setting"]')).not.toBeNull();
    expect(container.querySelector('img[data-icon="setting"]')).toBeNull();
  });

  it('hides the icon after a load error', () => {
    const { container } = render(<AppIcon name="missing-icon" />);

    const img = container.querySelector(
      'img[data-icon-loader="missing-icon"]',
    ) as HTMLImageElement | null;

    expect(img).not.toBeNull();

    fireEvent.error(img as HTMLImageElement);

    expect(container.querySelector('[data-icon="missing-icon"]')).toBeNull();
  });

  it('resets the hidden state when the name changes', () => {
    const { container, rerender } = render(<AppIcon name="missing-icon" />);

    const img = container.querySelector(
      'img[data-icon-loader="missing-icon"]',
    ) as HTMLImageElement | null;

    fireEvent.error(img as HTMLImageElement);
    rerender(<AppIcon name="provider" />);

    const nextIcon = container.querySelector('[data-icon="provider"]') as HTMLSpanElement | null;

    expect(nextIcon).not.toBeNull();
    expect(nextIcon?.style.maskImage).toContain('/icons/provider.svg');
  });
});
