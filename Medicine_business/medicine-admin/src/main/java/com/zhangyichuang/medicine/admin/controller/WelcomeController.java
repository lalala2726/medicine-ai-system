package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/4
 */
@RestController
@RequestMapping("/")
@Tag(name = "首页")
public class WelcomeController extends BaseController {


    @GetMapping
    @Anonymous
    public AjaxResult<Void> index() {
        return success();
    }
}
