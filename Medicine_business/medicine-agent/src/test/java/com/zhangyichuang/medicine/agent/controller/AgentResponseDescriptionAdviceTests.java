package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.agent.advice.AgentResponseDescriptionAdvice;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentResponseDescriptionAdviceTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AgentResponseDescriptionAdvice advice =
                new AgentResponseDescriptionAdvice(new AgentVoDescriptionResolver(), objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DescriptionController())
                .setControllerAdvice(advice)
                .build();
    }

    @Test
    void objectResponse_ShouldAppendMetaToData() throws Exception {
        mockMvc.perform(get("/agent/admin/desc/object"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试对象"))
                .andExpect(jsonPath("$.data.status.value").value(1))
                .andExpect(jsonPath("$.data.status.description").value("上架"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("演示对象"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.name").value("名称"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.status").value("状态"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions['detail.id']").value("明细ID"));
    }

    @Test
    void listResponse_ShouldWrapRowsAndMeta() throws Exception {
        mockMvc.perform(get("/agent/admin/desc/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].name").value("测试对象"))
                .andExpect(jsonPath("$.data.rows[0].status.value").value(1))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("演示对象"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions['detail.id']").value("明细ID"));
    }

    @Test
    void tableResponse_ShouldKeepPageFieldsAndAppendMeta() throws Exception {
        mockMvc.perform(get("/agent/admin/desc/table"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.rows[0].status.description").value("上架"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("演示对象"));
    }

    @Test
    void emptyListResponse_ShouldKeepOriginalShape() throws Exception {
        mockMvc.perform(get("/agent/admin/desc/empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void nonAnnotatedResponse_ShouldNotAppendMeta() throws Exception {
        mockMvc.perform(get("/agent/admin/desc/plain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.meta").doesNotExist());
    }

    @RestController
    @RequestMapping("/agent/admin/desc")
    static class DescriptionController {

        @GetMapping("/object")
        public AjaxResult<DemoVo> object() {
            return AjaxResult.success(buildDemo());
        }

        @GetMapping("/list")
        public AjaxResult<List<DemoVo>> list() {
            return AjaxResult.success(List.of(buildDemo()));
        }

        @GetMapping("/table")
        public AjaxResult<TableDataResult> table() {
            List<DemoVo> rows = List.of(buildDemo());
            TableDataResult tableDataResult = new TableDataResult(rows, 1L, 10L, 1L, new LinkedHashMap<>());
            return AjaxResult.success(tableDataResult);
        }

        @GetMapping("/empty")
        public AjaxResult<List<DemoVo>> empty() {
            return AjaxResult.success(List.of());
        }

        @GetMapping("/plain")
        public AjaxResult<Map<String, Object>> plain() {
            return AjaxResult.success(Map.of("ok", true));
        }

        private DemoVo buildDemo() {
            DemoDetailVo detail = new DemoDetailVo();
            detail.setId(99L);

            DemoVo vo = new DemoVo();
            vo.setName("测试对象");
            vo.setStatus(1);
            vo.setDetail(detail);
            return vo;
        }
    }

    @Data
    @FieldDescription(description = "演示对象")
    static class DemoVo {

        @FieldDescription(description = "名称")
        private String name;

        @FieldDescription(description = "状态")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_PRODUCT_STATUS)
        private Integer status;

        @FieldDescription(description = "明细")
        private DemoDetailVo detail;
    }

    @Data
    @FieldDescription(description = "演示明细")
    static class DemoDetailVo {

        @FieldDescription(description = "明细ID")
        private Long id;
    }
}
