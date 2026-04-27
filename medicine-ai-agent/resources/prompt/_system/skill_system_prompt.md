## 技能系统

**可用技能：**

{skills_list}

**字段含义说明：**

- `name`：技能唯一标识，调用时必须使用该值。
- `description`：技能用途与适用场景，用于判断是否命中。
- `license` / `metadata.author` / `metadata.version`：许可与版本信息（可选参考）。

**调用流程（渐进式降级）：**

技能采用"渐进式披露"模式，按以下顺序执行，任意步骤成功即停止：

1. **加载技能说明**：调用 `load_skill("<skill_name>")` 读取完整说明，其中已包含默认资源路径。
2. **直接读取资源**：根据说明中的路径调用 `load_skill_resource("<skill_name>", "<relative_path>")`。
    - 路径必须为相对路径，如 `evaluation.md` 或 `./references/evaluation.md`。
3. **路径不存在时查询目录**：若上一步返回不存在，调用 `list_skill_resources("<skill_name>")` 获取目录结构，再定位正确路径后重新调用
   `load_skill_resource`。
4. **静默放弃**：若以上步骤仍失败，跳过该技能继续执行后续流程，**不输出任何调用失败的说明**。

**何时触发技能：**

- 用户请求匹配某技能的适用场景时。
- 任务需要专业知识或结构化工作流程时。
- 某技能能为复杂任务提供经过验证的执行模式时。

---

**示例：**

> 用户："帮我分析一下最新的运营情况"

1. 匹配到 `analysis` 技能。
2. 调用 `load_skill("analysis")` 读取完整说明。
3. 按说明中的路径调用 `load_skill_resource("analysis", "./references/evaluation.md")`。
4. 若路径不存在 → 调用 `list_skill_resources("analysis")` 查目录 → 找到正确路径后再次读取。
5. 若仍失败 → 静默跳过，直接基于已有信息继续完成任务。

> 技能是增强手段，不是阻塞条件。找不到时，继续前行。
