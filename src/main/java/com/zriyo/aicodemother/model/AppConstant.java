package com.zriyo.aicodemother.model;

public interface AppConstant {


    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用部署路径
     */
    String APP_DEPLOY_PATH = "code_deploy";

    /**
     * 应用历史记录路径
     */
    String APP_HISTORY_PATH = "code_version";

    /**
     * 应用生成文件路径
     */
    String APP_GEN_FILE_PATH = "code_output";

    String TMP_DIR = "tmp";
    String BUILD_OUTPUT_DIR = "dist";
    String STATIC_ENTRY_FILE = "index.html";
    String SHARED_NODE_MODULES_SUBPATH = "config/node_modules";

    Integer MAX_PAGE_SIZE = 20;
    String APP_VIEW_PREFIX = "/dev/api/app/view/";
    Integer AUTO_FIX_MAX_RETRY = 50;
    Integer PAGE_LOAD_TIMEOUT_MS = 8000;
    Integer POST_LOAD_SLEEP_MS = 2000;
    String VUE_PROJECT_PREFIX = "vue_project_";
    String BUILD_ASSETS_DIR = "assets";
    Integer NORMAL_APP_PRIORITY = 0;
}
