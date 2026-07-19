package com.hotchpotch.lottery.user.controller;

import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.user.record.UserProfileResponse;
import com.hotchpotch.lottery.user.record.UserProfileUpdateRequest;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口。
 */
@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserContext currentUserContext;
    private final AuthSessionService authSessionService;

    public UserProfileController(
            UserProfileService userProfileService,
            CurrentUserContext currentUserContext,
            AuthSessionService authSessionService) {
        this.userProfileService = userProfileService;
        this.currentUserContext = currentUserContext;
        this.authSessionService = authSessionService;
    }

    /**
     * 查询当前用户个人中心资料。
     */
    @GetMapping("/detail")
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userProfileService.getProfile(currentUserContext.requireUserId()));
    }

    /**
     * 修改当前用户基础资料。
     */
    @PostMapping("/update")
    public ApiResponse<UserProfileResponse> updateProfile(
            @RequestBody(required = false) UserProfileUpdateRequest request) {
        UserProfileResponse response = userProfileService.updateProfile(currentUserContext.requireUserId(), request);
        authSessionService.updateSessionProfile(
                currentUserContext.requireToken(),
                response.nickname(),
                response.avatarUrl());

        return ApiResponse.success(response);
    }
}
