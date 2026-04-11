package com.bupt.tarecruit.common.init;

import com.bupt.tarecruit.admin.dao.AdminAccountDao;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * 应用启动监听器。
 * 在应用启动时执行初始化操作。
 */
@WebListener
public class AppStartupListener implements ServletContextListener {

    private final AdminAccountDao adminDao = new AdminAccountDao();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[APP-STARTUP] 应用启动中...");

        // 初始化默认管理员账号
        initDefaultAdmin();
    }

    /**
     * 初始化默认管理员账号（如果不存在）
     */
    private void initDefaultAdmin() {
        try {
            // 检查是否已有管理员账号
            AdminAccountDao.ProfileResult checkResult = adminDao.getProfileSettings("ADMIN-00001");
            if (checkResult.isSuccess()) {
                System.out.println("[APP-STARTUP] 管理员账号已存在，跳过初始化");
                return;
            }

            // 创建默认管理员账号
            AdminAccountDao.AdminRegisterResult result = adminDao.register(
                    "ADMIN-00001",
                    "Administrator",
                    "admin",
                    "admin@bupt.edu.cn",
                    "",
                    "admin123",
                    "System Administration",
                    java.util.List.of(
                            "users:read", "users:write",
                            "courses:read", "courses:write",
                            "notices:read", "notices:write",
                            "settings:read", "settings:write"
                    )
            );

            if (result.isSuccess()) {
                System.out.println("[APP-STARTUP] 默认管理员账号创建成功");
                System.out.println("[APP-STARTUP]   账号: admin");
                System.out.println("[APP-STARTUP]   密码: admin123");
                System.out.println("[APP-STARTUP]   请首次登录后立即修改密码！");
            } else {
                System.out.println("[APP-STARTUP] 管理员账号初始化失败: " + result.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[APP-STARTUP] 管理员账号初始化异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[APP-STARTUP] 应用已关闭");
    }
}
