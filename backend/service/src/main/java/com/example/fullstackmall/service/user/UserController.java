package com.example.fullstackmall.service.user;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.user.IUserFacade;
import com.example.fullstackmall.contract.user.UserQueryRequest;
import com.example.fullstackmall.contract.user.UserSummaryResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户查询 HTTP 入口。
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户")
public class UserController {

    @Resource
    private IUserFacade userFacade;

    @GetMapping
    @Operation(summary = "分页查询用户")
    public ApiResponse<PageResponse<UserSummaryResponse>> queryUsers(
            @Valid @ParameterObject UserQueryRequest request,
            HttpServletRequest servletRequest
    ) {
        PageResponse<UserSummaryResponse> response = userFacade.queryUsers(request);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }
}
