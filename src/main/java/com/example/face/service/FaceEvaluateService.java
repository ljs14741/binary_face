package com.example.bitcoin.face.service;

import com.example.bitcoin.face.repository.FaceEvaluateRepository;
import com.example.bitcoin.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import com.example.bitcoin.face.entity.FaceEvaluate;

@Service
@Slf4j
public class FaceEvaluateService {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private FaceEvaluateRepository faceEvaluateRepository;

    private static final int IMAGE_TOKEN_COST_LOW = 85;

    public List<String> evaluateImages(List<MultipartFile> images) throws IOException {
        return images.stream().map(image -> {
            try {
                byte[] imageData = image.getBytes();
                String response = openAIService.processRequestWithImage(imageData);

                // 🔹 JSON 응답을 파싱하기 전에 `error` 필드가 있는지 확인
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.has("error")) {
                    String errorMessage = jsonObject.getString("error");
                    log.warn("🚨 OpenAI API 에러 발생: " + errorMessage);
                    return "{\"error\": \"" + errorMessage + "\"}";  // 그대로 클라이언트에게 전달
                }

                return response;  // 정상 응답이면 기존 로직 유지
            } catch (IOException e) {
                log.error("이미지 처리 중 오류 발생", e);
                return "{\"error\": \"internal_server_error\"}";  // 내부 서버 오류 반환
            }
        }).collect(Collectors.toList());
    }


    // 금액 차감을 비동기로 처리하는 메서드

    @Async
    public void updateRemainingBalanceAsync(List<String> evaluations) {
        BigDecimal usageCost = calculateUsageCost(evaluations);
        updateRemainingBalance(usageCost);
    }

    // 토큰 사용량에 따라 금액을 계산하는 메서드
    private BigDecimal calculateUsageCost(List<String> evaluations) {
        BigDecimal totalCost = BigDecimal.ZERO;

        for (String evaluation : evaluations) {
            JSONObject jsonObject = new JSONObject(evaluation);
            JSONObject usage = jsonObject.getJSONObject("usage");

            int promptTokens = usage.getInt("prompt_tokens");
            int completionTokens = usage.getInt("completion_tokens");

            // 입력 토큰 비용 계산
            BigDecimal inputCost = new BigDecimal(promptTokens)
                    .divide(new BigDecimal(1_000_000), 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.15"))
                    .setScale(5, RoundingMode.HALF_UP);
            log.info("Input Cost (prompt tokens): {}", inputCost);

            // 출력 토큰 비용 계산
            BigDecimal outputCost = new BigDecimal(completionTokens)
                    .divide(new BigDecimal(1_000_000), 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.60"))
                    .setScale(5, RoundingMode.HALF_UP);
            log.info("Output Cost (completion tokens): {}", outputCost);

            // 텍스트 비용 합산
            totalCost = totalCost.add(inputCost).add(outputCost).setScale(5, RoundingMode.HALF_UP);
        }

        // 이미지 비용 추가 (low 모드로 가정하여 85 토큰으로 고정)
        BigDecimal imageCost = new BigDecimal(IMAGE_TOKEN_COST_LOW)
                .divide(new BigDecimal(1_000_000), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.15"))
                .setScale(5, RoundingMode.HALF_UP);
        log.info("Image Cost (low-res fixed 85 tokens): {}", imageCost);

        // 총 비용 계산
        totalCost = totalCost.add(imageCost).setScale(5, RoundingMode.HALF_UP);
        log.info("Total Usage Cost Calculated (including low-res image cost): {}", totalCost);

        return totalCost;
    }

    // 계산된 금액을 사용해 DB의 잔액을 업데이트하는 메서드
    public void updateRemainingBalance(BigDecimal usageCost) {
        FaceEvaluate faceEvaluate = faceEvaluateRepository.findAll().stream().findFirst().orElse(null);

        if (faceEvaluate != null) {
            BigDecimal currentBalance = faceEvaluate.getRemainingBalance();
            BigDecimal newBalance = currentBalance.subtract(usageCost).setScale(5, RoundingMode.HALF_UP);

            // 로그로 현재 상태 출력
            log.info("Current Balance: {}", currentBalance);
            log.info("Usage Cost: {}", usageCost);
            log.info("New Balance (to be updated): {}", newBalance);

            // 변경된 값 설정
            faceEvaluate.setRemainingBalance(newBalance);

            // 변경 사항 저장
            faceEvaluateRepository.save(faceEvaluate);
            log.info("Remaining balance updated successfully.");
        } else {
            log.warn("FaceEvaluate entity not found.");
        }
    }

    // 잔액을 조회하는 메서드
    public BigDecimal getCurrentBalance() {
        return faceEvaluateRepository.findAll()
                .stream()
                .findFirst()
                .map(FaceEvaluate::getRemainingBalance)
                .orElse(BigDecimal.ZERO);
    }
}
