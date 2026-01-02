package com.zriyo.aicodemother.controller.Admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.DeleteRequest;
import com.zriyo.aicodemother.common.PageRequest;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.constant.UserConstant;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.entity.PointsCode;
import com.zriyo.aicodemother.service.PointsCodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台积分码控制层 (MyBatis-Flex 优化版)
 */
@RestController
@RequestMapping("/admin/points")
@SaCheckRole(UserConstant.ADMIN_ROLE)
public class AdminPointsController {

    @Resource
    private PointsCodeService pointsCodeService;


    /**
     * 分页获取积分码列表（仅管理员）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PointsCode>> listPointsCodeByPage(@RequestBody PageRequest pageRequest) {
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = pageRequest.getPageNum();
        long pageSize = pageRequest.getPageSize();

        // 排序处理
        QueryWrapper wrapper = QueryWrapper.create();
        String sortField = pageRequest.getSortField();
        if (StrUtil.isNotBlank(sortField)) {
            // 针对 PointsCode 特殊处理映射：前端传 createTime，数据库是 created_at
            if ("createTime".equals(sortField)) {
                sortField = "created_at";
            } else {
                sortField = StrUtil.toUnderlineCase(sortField);
            }
            wrapper.orderBy(sortField, "ascend".equals(pageRequest.getSortOrder()));
        }

        Page<PointsCode> page = pointsCodeService.page(Page.of(pageNum, pageSize), wrapper);
        return ResultUtils.success(page);
    }


    /**
     * 批量生成积分码
     */
    @PostMapping("/generate")
    public BaseResponse<Boolean> generatePointsCode(int count, int points, int expireDays) {
        ThrowUtils.throwIf(count <= 0 || count > 100, ErrorCode.PARAMS_ERROR, "生成数量需在 1-100 之间");
        ThrowUtils.throwIf(points <= 0, ErrorCode.PARAMS_ERROR, "积分需大于 0");

        long loginId = StpUtil.getLoginIdAsLong();
        LocalDateTime expireTime = LocalDateTime.now().plusDays(expireDays);

        List<PointsCode> codeList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PointsCode pointsCode = new PointsCode();
            pointsCode.setCode(RandomUtil.randomString(8).toUpperCase());
            pointsCode.setPoints(points);
            pointsCode.setStatus(0);
            pointsCode.setCreatedBy(loginId);
            pointsCode.setCreatedAt(LocalDateTime.now());
            pointsCode.setExpiredAt(expireTime);
            codeList.add(pointsCode);
        }

        boolean result = pointsCodeService.saveBatch(codeList);
        return ResultUtils.success(result);
    }

    /**
     * 作废积分码
     */
    @PostMapping("/void")
    public BaseResponse<Boolean> voidPointsCode(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        PointsCode pointsCode = pointsCodeService.getById(id);
        ThrowUtils.throwIf(pointsCode == null, ErrorCode.NOT_FOUND_ERROR);

        if (pointsCode.getStatus() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有未使用的积分码可以作废");
        }

        pointsCode.setStatus(2); // 2=已销毁/已过期
        boolean result = pointsCodeService.updateById(pointsCode);
        return ResultUtils.success(result);
    }

    /**
     * 删除积分码
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePointsCode(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = pointsCodeService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }
}
