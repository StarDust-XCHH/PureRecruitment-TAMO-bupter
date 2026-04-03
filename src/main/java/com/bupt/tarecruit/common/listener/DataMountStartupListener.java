package com.bupt.tarecruit.common.listener;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * 应用启动时打印数据挂载目录信息，便于部署后确认 JSON 数据根路径。
 * 先输出 {@link DataMountPaths} 的权威结果，再输出 TA 侧说明（内容有重叠时可对照）。
 */
@WebListener
public class DataMountStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[data-mount] root=" + DataMountPaths.root()
                + " fromEnvironment=" + DataMountPaths.fromEnvironment()
                + " env=" + DataMountPaths.DATA_MOUNT_ENV);
        System.out.println(TaAccountDao.getDataMountStatusMessage());
    }

}
