/**
 * 图表 JSON 解析工具
 *
 * 区分流式传输中数据不完整（应显示加载态）和最终格式错误（应显示错误提示）。
 */

/**
 * 尝试解析图表 JSON 数据。
 *
 * - 内容为空或过短：返回 null（显示加载态）
 * - 解析成功：返回解析后对象
 * - 内容看起来已完整但解析失败：抛出 Error（显示错误提示）
 * - 内容不完整（还在流式传输中）：返回 null（显示加载态）
 */
export function parseChartJSON(rawContent: string, minLength = 10): any | null {
  const content = String(rawContent).trim();

  if (!content || content.length < minLength) return null;

  try {
    return JSON.parse(content);
  } catch {
    // 花括号/方括号匹配且以闭合符号结尾 → 内容已完整，属于真正的格式错误
    const looksComplete = content.endsWith('}') || content.endsWith(']');
    if (looksComplete) {
      throw new Error('图表数据格式不正确，无法解析 JSON');
    }
    // 否则认为是流式传输中，数据尚不完整
    return null;
  }
}
