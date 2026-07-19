package com.hotchpotch.lottery.favorite.service;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.FavoriteProperties;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.enums.LotteryNumberFavoriteStatus;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteCreateRequest;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoritePageResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteUpdateRequest;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户收藏号码服务。
 */
@Service
public class LotteryNumberFavoriteService {

    private static final int FAVORITE_NAME_MAX_LENGTH = 30;
    private static final int REMARK_MAX_LENGTH = 200;

    private final LotteryNumberFavoriteRepository favoriteRepository;
    private final LotteryDltNumberService numberService;
    private final FavoriteProperties favoriteProperties;

    public LotteryNumberFavoriteService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryDltNumberService numberService,
            FavoriteProperties favoriteProperties) {
        this.favoriteRepository = favoriteRepository;
        this.numberService = numberService;
        this.favoriteProperties = favoriteProperties;
    }

    /**
     * 新增收藏号码；同一用户相同号码已存在时保持幂等。
     */
    @Transactional
    public LotteryNumberFavoriteResponse createFavorite(Long userId, LotteryNumberFavoriteCreateRequest request) {
        requireUserId(userId);
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏请求体不能为空");
        }

        String lotteryType = normalizeLotteryType(request.lotteryType());
        LotteryDltNumber normalizedNumber = normalizeDltNumber(request.frontNumbers(), request.backNumbers());
        String frontNumbers = storeNumbers(normalizedNumber.frontNumbers());
        String backNumbers = storeNumbers(normalizedNumber.backNumbers());
        String favoriteName = normalizeFavoriteName(request.favoriteName());
        String remark = normalizeRemark(request.remark());

        return favoriteRepository.findByUserAndNumbers(userId, lotteryType, frontNumbers, backNumbers)
                .map(existing -> activateExistingFavorite(existing, favoriteName, remark))
                .orElseGet(() -> createNewFavorite(
                        userId,
                        lotteryType,
                        frontNumbers,
                        backNumbers,
                        favoriteName,
                        remark));
    }

    /**
     * 分页查询当前用户收藏号码。
     */
    public LotteryNumberFavoritePageResponse listFavorites(
            Long userId,
            int pageNo,
            int pageSize,
            String status,
            String keyword) {
        requireUserId(userId);
        int safePageNo = Math.max(pageNo, PageConstants.DEFAULT_PAGE_NO);
        int safePageSize = Math.min(Math.max(pageSize, 1), PageConstants.MAX_PAGE_SIZE);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedKeyword = trimToNull(keyword);
        Long total = favoriteRepository.countByUserIdAndStatusAndKeyword(userId, normalizedStatus, normalizedKeyword);
        List<LotteryNumberFavoriteResponse> favorites = favoriteRepository
                .findPageByUserIdAndStatus(userId, normalizedStatus, normalizedKeyword, safePageNo, safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);

        return new LotteryNumberFavoritePageResponse(
                safePageNo,
                safePageSize,
                total,
                pages,
                normalizedStatus,
                normalizedKeyword,
                favorites);
    }

    /**
     * 查询当前用户的一条收藏号码。
     */
    public LotteryNumberFavoriteResponse getFavorite(Long userId, Long favoriteId) {
        return toResponse(findOwnedFavorite(userId, favoriteId));
    }

    /**
     * 修改当前用户收藏号码的名称和备注，不允许修改号码本身。
     */
    @Transactional
    public LotteryNumberFavoriteResponse updateFavorite(
            Long userId,
            Long favoriteId,
            LotteryNumberFavoriteUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏修改请求体不能为空");
        }

        LotteryNumberFavorite favorite = findOwnedFavorite(userId, favoriteId);
        favorite.setFavoriteName(normalizeFavoriteName(request.favoriteName()));
        favorite.setRemark(normalizeRemark(request.remark()));
        favoriteRepository.updateById(favorite);

        return toResponse(favorite);
    }

    /**
     * 取消当前用户的一条收藏号码；重复取消保持幂等。
     */
    @Transactional
    public LotteryNumberFavoriteResponse deactivateFavorite(Long userId, Long favoriteId) {
        LotteryNumberFavorite favorite = findOwnedFavorite(userId, favoriteId);
        if (LotteryNumberFavoriteStatus.CANCELLED.code().equals(favorite.getStatus())) {
            return toResponse(favorite);
        }

        favorite.setStatus(LotteryNumberFavoriteStatus.CANCELLED.code());
        favorite.setCancelTime(LocalDateTime.now());
        favoriteRepository.updateById(favorite);

        return toResponse(favorite);
    }

    /**
     * 重新启用当前用户的一条收藏号码；重复启用保持幂等。
     */
    @Transactional
    public LotteryNumberFavoriteResponse activateFavorite(Long userId, Long favoriteId) {
        LotteryNumberFavorite favorite = findOwnedFavorite(userId, favoriteId);
        if (LotteryNumberFavoriteStatus.ACTIVE.code().equals(favorite.getStatus())) {
            return toResponse(favorite);
        }

        ensureActiveFavoriteLimit(userId);
        favorite.setStatus(LotteryNumberFavoriteStatus.ACTIVE.code());
        favorite.setEffectiveTime(LocalDateTime.now());
        favorite.setCancelTime(null);
        favoriteRepository.updateById(favorite);

        return toResponse(favorite);
    }

    /**
     * 复用已存在收藏；已取消记录会被重新启用。
     */
    private LotteryNumberFavoriteResponse activateExistingFavorite(
            LotteryNumberFavorite favorite,
            String favoriteName,
            String remark) {
        boolean changed = false;
        if (!LotteryNumberFavoriteStatus.ACTIVE.code().equals(favorite.getStatus())) {
            ensureActiveFavoriteLimit(favorite.getUserId());
            favorite.setStatus(LotteryNumberFavoriteStatus.ACTIVE.code());
            favorite.setEffectiveTime(LocalDateTime.now());
            favorite.setCancelTime(null);
            changed = true;
        }
        if (favoriteName != null) {
            favorite.setFavoriteName(favoriteName);
            changed = true;
        }
        if (remark != null) {
            favorite.setRemark(remark);
            changed = true;
        }
        if (changed) {
            favoriteRepository.updateById(favorite);
        }

        return toResponse(favorite);
    }

    /**
     * 创建新的有效收藏记录。
     */
    private LotteryNumberFavoriteResponse createNewFavorite(
            Long userId,
            String lotteryType,
            String frontNumbers,
            String backNumbers,
            String favoriteName,
            String remark) {
        ensureActiveFavoriteLimit(userId);
        LocalDateTime now = LocalDateTime.now();
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        favorite.setUserId(userId);
        favorite.setLotteryType(lotteryType);
        favorite.setFrontNumbers(frontNumbers);
        favorite.setBackNumbers(backNumbers);
        favorite.setFavoriteName(favoriteName);
        favorite.setRemark(remark);
        favorite.setStatus(LotteryNumberFavoriteStatus.ACTIVE.code());
        favorite.setFavoriteTime(now);
        favorite.setEffectiveTime(now);
        favoriteRepository.insert(favorite);

        return toResponse(favorite);
    }

    /**
     * 查询并校验收藏归属；不存在或不属于当前用户都返回不存在，避免泄露数据。
     */
    private LotteryNumberFavorite findOwnedFavorite(Long userId, Long favoriteId) {
        requireUserId(userId);
        if (favoriteId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏 ID 不能为空");
        }

        LotteryNumberFavorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(this::favoriteNotFound);
        if (!userId.equals(favorite.getUserId())) {
            throw favoriteNotFound();
        }

        return favorite;
    }

    /**
     * 校验当前用户有效收藏数量未超过上限。
     */
    private void ensureActiveFavoriteLimit(Long userId) {
        Long activeCount = favoriteRepository.countByUserIdAndStatus(
                userId,
                LotteryNumberFavoriteStatus.ACTIVE.code());
        if (activeCount >= favoriteProperties.maxActiveCount()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "有效收藏数量已达上限，请先取消不再跟踪的收藏");
        }
    }

    /**
     * 校验并规范化彩票类型。
     */
    private String normalizeLotteryType(String lotteryType) {
        String normalizedLotteryType = trimToNull(lotteryType);
        if (normalizedLotteryType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "彩票类型不能为空");
        }
        if (!LotteryType.DLT.code().equals(normalizedLotteryType.toUpperCase())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "当前仅支持大乐透收藏");
        }

        return LotteryType.DLT.code();
    }

    /**
     * 复用大乐透号码校验服务生成规范化号码。
     */
    private LotteryDltNumber normalizeDltNumber(List<Integer> frontNumbers, List<Integer> backNumbers) {
        if (frontNumbers == null || backNumbers == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏号码不能为空");
        }

        return numberService.parseSingle(numberInputText(frontNumbers, backNumbers));
    }

    /**
     * 将请求中的号码数组转换成现有解析服务可识别的文本。
     */
    private String numberInputText(List<Integer> frontNumbers, List<Integer> backNumbers) {
        return frontNumbers.stream().map(String::valueOf).collect(Collectors.joining(" "))
                + " + "
                + backNumbers.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    /**
     * 将号码列表格式化成数据库存储用的两位数字逗号分隔文本。
     */
    private String storeNumbers(List<Integer> numbers) {
        return numbers.stream()
                .map(number -> String.format("%02d", number))
                .collect(Collectors.joining(","));
    }

    /**
     * 校验并规范化收藏名称。
     */
    private String normalizeFavoriteName(String favoriteName) {
        String normalizedFavoriteName = trimToNull(favoriteName);
        if (normalizedFavoriteName != null && normalizedFavoriteName.length() > FAVORITE_NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏名称不能超过 30 个字符");
        }

        return normalizedFavoriteName;
    }

    /**
     * 校验并规范化备注。
     */
    private String normalizeRemark(String remark) {
        String normalizedRemark = trimToNull(remark);
        if (normalizedRemark != null && normalizedRemark.length() > REMARK_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "备注不能超过 200 个字符");
        }

        return normalizedRemark;
    }

    /**
     * 校验并规范化可选状态筛选。
     */
    private String normalizeOptionalStatus(String status) {
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            return null;
        }
        try {
            return LotteryNumberFavoriteStatus.valueOf(normalizedStatus.toUpperCase()).code();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏状态不合法");
        }
    }

    /**
     * 组装接口响应。
     */
    private LotteryNumberFavoriteResponse toResponse(LotteryNumberFavorite favorite) {
        return new LotteryNumberFavoriteResponse(
                favorite.getId(),
                favorite.getLotteryType(),
                favorite.getFrontNumbers(),
                favorite.getBackNumbers(),
                displayText(favorite),
                displayName(favorite),
                favorite.getRemark(),
                favorite.getStatus(),
                favorite.getFavoriteTime(),
                favorite.getEffectiveTime(),
                favorite.getCancelTime());
    }

    /**
     * 收藏名称为空时使用默认号码名称。
     */
    private String displayName(LotteryNumberFavorite favorite) {
        if (favorite.getFavoriteName() != null && !favorite.getFavoriteName().isBlank()) {
            return favorite.getFavoriteName();
        }

        return "大乐透 " + displayText(favorite);
    }

    /**
     * 生成展示用号码文本。
     */
    private String displayText(LotteryNumberFavorite favorite) {
        return favorite.getFrontNumbers().replace(",", " ")
                + " + "
                + favorite.getBackNumbers().replace(",", " ");
    }

    /**
     * 校验用户 ID 已从认证上下文恢复。
     */
    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 去除文本前后空白；空字符串统一转为 null。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BusinessException favoriteNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收藏号码不存在");
    }
}
