package com.bupt.tarecruit.common.listener;

import com.bupt.tarecruit.ta.dao.TaAccountDao;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DataMountStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println(TaAccountDao.getDataMountStatusMessage());
    }

}
