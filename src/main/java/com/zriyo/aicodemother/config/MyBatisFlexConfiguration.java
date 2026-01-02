package com.zriyo.aicodemother.config;

import com.mybatisflex.core.audit.AuditManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app", name = "sql-audit-enabled", havingValue = "true", matchIfMissing = false)
public class MyBatisFlexConfiguration {

    private static final Logger logger = LoggerFactory
        .getLogger("mybatis-flex-sql");


    public MyBatisFlexConfiguration() {
        //开启审计功能
        AuditManager.setAuditEnable(false);

        //设置 SQL 审计收集器
        AuditManager.setMessageCollector(auditMessage ->
            logger.info("{},{}ms", auditMessage.getFullSql()
                , auditMessage.getElapsedTime())
        );
        logger.info("开启 SQL 审计功能");
    }
}
