package com.zriyo.aicodemother.controller.Admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.constant.UserConstant;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.dto.app.AppQueryRequest;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 后台应用控制层 (MyBatis-Flex 优化版)
 */
@RestController
@RequestMapping("/admin/app")
@SaCheckRole(UserConstant.ADMIN_ROLE)
public class AdminAppController {

    @Resource
    private AppService appService;


    /**
     * 分页获取应用列表（仅管理员，语法已加固）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();

        // 构造管理员专用的 QueryWrapper，加入严谨的 condition 判断
        QueryWrapper adminWrapper = QueryWrapper.create()
                .like("appName", appQueryRequest.getAppName(), StrUtil.isNotBlank(appQueryRequest.getAppName()))
                .eq("is_published", appQueryRequest.getIsPublished(), appQueryRequest.getIsPublished() != null)
                .eq("priority", appQueryRequest.getPriority(), appQueryRequest.getPriority() != null);

        if (StrUtil.isNotBlank(appQueryRequest.getSortField())) {
            adminWrapper.orderBy(appQueryRequest.getSortField(), "ascend".equals(appQueryRequest.getSortOrder()));
        }
        adminWrapper.orderBy(App::getPriority,false);
        adminWrapper.orderBy(App::getCreateTime,false);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), adminWrapper);
        return ResultUtils.success(appPage);
    }




    /**
     * 发布/下架应用
     */
    @PostMapping("/publish")
    public BaseResponse<Boolean> publishApp(long id, boolean publish) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        app.setIsPublished(publish ? 1 : 0);
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }

    /**
     * 设置应用为精选
     */
    @PostMapping("/priority")
    public BaseResponse<Boolean> setAppPriority(long id, boolean priority) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        if (priority) {
            app.setPriority(AppConstant.GOOD_APP_PRIORITY);
        }else {
            app.setPriority(AppConstant.NORMAL_APP_PRIORITY);
        }
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }
    /**
     * 根据 id 获取应用
     */
    @GetMapping("/get")
    public BaseResponse<App> getAppById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

    /**
     * 删除应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(Long  id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }
}
