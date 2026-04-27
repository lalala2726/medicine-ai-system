import { buildConversationPath, parseConversationIdFromSplat } from '../_useChat';

describe('SmartAssistant useChat url helpers', () => {
  describe('parseConversationIdFromSplat', () => {
    it('returns undefined for empty splat', () => {
      expect(parseConversationIdFromSplat(undefined)).toBeUndefined();
      expect(parseConversationIdFromSplat('')).toBeUndefined();
      expect(parseConversationIdFromSplat('   ')).toBeUndefined();
      expect(parseConversationIdFromSplat('/')).toBeUndefined();
    });

    it('extracts first path segment as conversation id', () => {
      expect(parseConversationIdFromSplat('abc')).toBe('abc');
      expect(parseConversationIdFromSplat('abc/extra')).toBe('abc');
      expect(parseConversationIdFromSplat('/abc/extra')).toBe('abc');
      expect(parseConversationIdFromSplat(' abc /extra')).toBe('abc');
    });
  });

  describe('buildConversationPath', () => {
    it('builds base path when id is missing', () => {
      expect(buildConversationPath(undefined)).toBe('/smart-assistant');
      expect(buildConversationPath('')).toBe('/smart-assistant');
    });

    it('builds conversation path when id exists', () => {
      expect(buildConversationPath('abc')).toBe('/smart-assistant/abc');
      expect(buildConversationPath('uuid-123')).toBe('/smart-assistant/uuid-123');
    });
  });
});
