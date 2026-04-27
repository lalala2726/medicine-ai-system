package com.zhangyichuang.medicine.agent;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.zhangyichuang.medicine")
@EnableTransactionManagement
@EnableAsync
@EnableDubbo
public class MedicineAgentApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String hint = """
                     _                    _     ____                               __       _   ____  _             _              \s
                    / \\   __ _  ___ _ __ | |_  / ___| _   _  ___ ___ ___  ___ ___ / _|_   _| | / ___|| |_ __ _ _ __| |_ _   _ _ __ \s
                   / _ \\ / _` |/ _ \\ '_ \\| __| \\___ \\| | | |/ __/ __/ _ \\/ __/ __| |_| | | | | \\___ \\| __/ _` | '__| __| | | | '_ \\\s
                  / ___ \\ (_| |  __/ | | | |_   ___) | |_| | (_| (_|  __/\\__ \\__ \\  _| |_| | |  ___) | || (_| | |  | |_| |_| | |_) |
                 /_/   \\_\\__, |\\___|_| |_|\\__| |____/ \\__,_|\\___\\___\\___||___/___/_|  \\__,_|_| |____/ \\__\\__,_|_|   \\__|\\__,_| .__/\s
                         |___/                                                                                               |_|   \s
                """;
        SpringApplication.run(MedicineAgentApplication.class, args);
        System.out.println(hint);
    }
}
