package com.hotchpotch.lottery.favorite.controller;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteCreateRequest;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoritePageResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteUpdateRequest;
import com.hotchpotch.lottery.favorite.service.LotteryNumberFavoriteService;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户收藏号码接口。
 */
@RestController
@RequestMapping("/api/lottery/favorites")
public class LotteryNumberFavoriteController {

    private final LotteryNumberFavoriteService favoriteService;
    private final CurrentUserContext currentUserContext;

    public LotteryNumberFavoriteController(
            LotteryNumberFavoriteService favoriteService,
            CurrentUserContext currentUserContext) {
        this.favoriteService = favoriteService;
        this.currentUserContext = currentUserContext;
    }

    /**
     * 新增收藏号码；重复收藏保持幂等。
     */
    @PostMapping("/create")
    public ApiResponse<LotteryNumberFavoriteResponse> createFavorite(
            @RequestBody(required = false) LotteryNumberFavoriteCreateRequest request) {
        return ApiResponse.success(favoriteService.createFavorite(currentUserContext.requireUserId(), request));
    }

    /**
     * 分页查询当前用户收藏号码。
     */
    @GetMapping("/page")
    public ApiResponse<LotteryNumberFavoritePageResponse> listFavorites(
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_NO_TEXT) int pageNo,
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_SIZE_TEXT) int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(favoriteService.listFavorites(
                currentUserContext.requireUserId(),
                pageNo,
                pageSize,
                status,
                keyword));
    }

    /**
     * 查询当前用户收藏号码详情。
     */
    @GetMapping("/{favoriteId}")
    public ApiResponse<LotteryNumberFavoriteResponse> getFavorite(
            @PathVariable Long favoriteId) {
        return ApiResponse.success(favoriteService.getFavorite(currentUserContext.requireUserId(), favoriteId));
    }

    /**
     * 修改当前用户收藏号码名称和备注。
     */
    @PostMapping("/update")
    public ApiResponse<LotteryNumberFavoriteResponse> updateFavorite(
            @RequestBody(required = false) LotteryNumberFavoriteUpdateRequest request) {
        Long favoriteId = request == null ? null : request.favoriteId();

        return ApiResponse.success(favoriteService.updateFavorite(currentUserContext.requireUserId(), favoriteId, request));
    }

    /**
     * 取消当前用户收藏号码。
     */
    @PostMapping("/deactivate")
    public ApiResponse<LotteryNumberFavoriteResponse> deactivateFavorite(
            @RequestBody(required = false) LotteryNumberFavoriteUpdateRequest request) {
        Long favoriteId = request == null ? null : request.favoriteId();

        return ApiResponse.success(favoriteService.deactivateFavorite(currentUserContext.requireUserId(), favoriteId));
    }

    /**
     * 重新启用当前用户收藏号码。
     */
    @PostMapping("/activate")
    public ApiResponse<LotteryNumberFavoriteResponse> activateFavorite(
            @RequestBody(required = false) LotteryNumberFavoriteUpdateRequest request) {
        Long favoriteId = request == null ? null : request.favoriteId();

        return ApiResponse.success(favoriteService.activateFavorite(currentUserContext.requireUserId(), favoriteId));
    }

    /**
     * 删除当前用户已取消收藏号码。
     */
    @PostMapping("/delete")
    public ApiResponse<Void> deleteFavorite(
            @RequestBody(required = false) LotteryNumberFavoriteUpdateRequest request) {
        Long favoriteId = request == null ? null : request.favoriteId();

        favoriteService.deleteFavorite(currentUserContext.requireUserId(), favoriteId);

        return ApiResponse.success(null);
    }
}
