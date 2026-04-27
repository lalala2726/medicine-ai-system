package com.zhangyichuang.medicine.admin;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.zhangyichuang.medicine"})
@MapperScan({"com.zhangyichuang.medicine.admin.mapper", "com.zhangyichuang.medicine.shared.mapper"})
@EnableTransactionManagement
@EnableAsync
@EnableDubbo
public class MedicineAdminApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MedicineAdminApplication.class, args);
        String hint = """
                     _       _           _         ____                               __       _   ____  _             _              \s
                    / \\   __| |_ __ ___ (_)_ __   / ___| _   _  ___ ___ ___  ___ ___ / _|_   _| | / ___|| |_ __ _ _ __| |_ _   _ _ __ \s
                   / _ \\ / _` | '_ ` _ \\| | '_ \\  \\___ \\| | | |/ __/ __/ _ \\/ __/ __| |_| | | | | \\___ \\| __/ _` | '__| __| | | | '_ \\\s
                  / ___ \\ (_| | | | | | | | | | |  ___) | |_| | (_| (_|  __/\\__ \\__ \\  _| |_| | |  ___) | || (_| | |  | |_| |_| | |_) |
                 /_/   \\_\\__,_|_| |_| |_|_|_| |_| |____/ \\__,_|\\___\\___\\___||___/___/_|  \\__,_|_| |____/ \\__\\__,_|_|   \\__|\\__,_| .__/\s
                                                                                                                                |_|   \s
                """;
        System.out.println(hint);
    }

}
