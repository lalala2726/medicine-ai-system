package com.zhangyichuang.medicine.admin.startup;

import com.zhangyichuang.medicine.admin.service.MallProductUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 商品单位默认数据初始化任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallProductUnitInitRunner implements ApplicationRunner {

    /**
     * 商品单位服务。
     */
    private final MallProductUnitService mallProductUnitService;

    /**
     * 应用启动回调。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        boolean initialized = mallProductUnitService.initializeDefaultUnitsIfNeeded();
        log.info("商品单位默认数据初始化完成, initialized={}", initialized);
    }
}
