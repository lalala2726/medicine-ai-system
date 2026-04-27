package com.zhangyichuang.medicine.admin.startup;

import com.zhangyichuang.medicine.admin.task.MallProductIndexRebuildCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 商品索引启动后重建触发器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallProductIndexStartupRebuildRunner {

    /**
     * 商品索引全量重建协调器。
     */
    private final MallProductIndexRebuildCoordinator mallProductIndexRebuildCoordinator;

    /**
     * 应用启动完成后检查是否需要后台重建商品索引。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始检查商品索引启动自动重建条件");
        mallProductIndexRebuildCoordinator.triggerStartupRebuildIfNeeded();
    }
}
