package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * 大乐透号码解析与校验服务。
 */
@Service
public class LotteryDltNumberService {

    private static final int FRONT_COUNT = 5;
    private static final int BACK_COUNT = 2;
    private static final int FRONT_MIN = 1;
    private static final int FRONT_MAX = 35;
    private static final int BACK_MIN = 1;
    private static final int BACK_MAX = 12;
    private static final int MAX_BATCH_SIZE = 50;

    /**
     * 解析并校验单注大乐透号码。
     */
    public LotteryDltNumber parseSingle(String input) {
        if (input == null || input.isBlank()) {
            throw invalid("号码不能为空");
        }

        String[] areas = input.trim().split("\\+");
        if (areas.length != 2) {
            throw invalid("号码格式不合法，请使用 + 分隔前区和后区");
        }

        List<Integer> frontNumbers = parseAreaNumbers(areas[0], "前区");
        List<Integer> backNumbers = parseAreaNumbers(areas[1], "后区");
        validateArea(frontNumbers, FRONT_COUNT, FRONT_MIN, FRONT_MAX, "前区");
        validateArea(backNumbers, BACK_COUNT, BACK_MIN, BACK_MAX, "后区");

        List<Integer> normalizedFrontNumbers = sorted(frontNumbers);
        List<Integer> normalizedBackNumbers = sorted(backNumbers);
        return new LotteryDltNumber(
                normalizedFrontNumbers,
                normalizedBackNumbers,
                displayText(normalizedFrontNumbers, normalizedBackNumbers));
    }

    /**
     * 批量解析并校验大乐透号码，错误信息中会带上行号。
     */
    public List<LotteryDltNumber> parseBatch(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw invalid("号码不能为空");
        }
        if (inputs.size() > MAX_BATCH_SIZE) {
            throw invalid("单次最多分析 50 注号码");
        }

        return java.util.stream.IntStream.range(0, inputs.size())
                .mapToObj(index -> parseLine(index + 1, inputs.get(index)))
                .toList();
    }

    /**
     * 解析批量输入中的单行号码。
     */
    private LotteryDltNumber parseLine(int lineNo, String input) {
        try {
            return parseSingle(input);
        } catch (BusinessException ex) {
            throw invalid("第" + lineNo + "行：" + ex.getMessage());
        }
    }

    /**
     * 解析前区或后区的号码列表。
     */
    private List<Integer> parseAreaNumbers(String areaText, String areaName) {
        List<String> tokens = Stream.of(areaText.trim().split("[,，\\s]+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (tokens.isEmpty()) {
            return List.of();
        }

        return tokens.stream()
                .map(token -> parseNumber(token, areaName))
                .toList();
    }

    /**
     * 将单个号码文本解析为整数。
     */
    private Integer parseNumber(String token, String areaName) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw invalid(areaName + "号码必须为数字");
        }
    }

    /**
     * 校验某个号码区的数量、范围和重复性。
     */
    private void validateArea(List<Integer> numbers, int expectedCount, int min, int max, String areaName) {
        if (numbers.size() != expectedCount) {
            throw invalid(areaName + "号码必须为 " + expectedCount + " 个");
        }

        for (Integer number : numbers) {
            if (number < min || number > max) {
                throw invalid(areaName + "号码范围必须为 " + formatNumber(min) + "-" + formatNumber(max));
            }
        }

        Set<Integer> uniqueNumbers = new HashSet<>(numbers);
        if (uniqueNumbers.size() != numbers.size()) {
            throw invalid(areaName + "号码不能重复");
        }
    }

    /**
     * 对号码列表升序排序。
     */
    private List<Integer> sorted(List<Integer> numbers) {
        return numbers.stream().sorted().toList();
    }

    /**
     * 生成前端展示使用的规范化号码文本。
     */
    private String displayText(List<Integer> frontNumbers, List<Integer> backNumbers) {
        return formatNumbers(frontNumbers) + " + " + formatNumbers(backNumbers);
    }

    /**
     * 将号码列表格式化为两位数字空格分隔文本。
     */
    private String formatNumbers(List<Integer> numbers) {
        return numbers.stream()
                .map(this::formatNumber)
                .collect(Collectors.joining(" "));
    }

    /**
     * 将号码格式化为两位数字。
     */
    private String formatNumber(Integer number) {
        return String.format("%02d", number);
    }

    /**
     * 创建参数非法业务异常。
     */
    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
