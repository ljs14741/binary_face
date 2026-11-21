package com.example.face.controller;

import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.HttpStatus;
import com.example.face.dto.FaceEvaluateDTO;
import com.example.face.service.FaceEvaluateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.example.face.service.VisitorService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class FaceEvaluateController {

    @Autowired
    private FaceEvaluateService faceEvaluateService;

    private final VisitorService visitorService;

    // 소개·문의·개인정보는 메인 한 곳에서 관리함 (영구 이동)
    @GetMapping({"/about", "/contact", "/privacy-policy"})
    public RedirectView legalPage(HttpServletRequest request) {
        RedirectView view = new RedirectView("https://binaryworld.kr" + request.getRequestURI());
        view.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return view;
    }

    @GetMapping("/")
    public String faceEvaluate(HttpServletRequest request, HttpServletResponse response) {
        visitorService.countVisitIfNeeded(request, response, "face");
        return "faceEvaluate";
    }

    @PostMapping("/api/evaluate")
    @ResponseBody
    public EvaluationResponse evaluateImages(@RequestParam("images") List<MultipartFile> images) {
        try {
            List<String> evaluations = faceEvaluateService.evaluateImages(images);
            log.info("데이터: " + evaluations);
            EvaluationResponse response = new EvaluationResponse(evaluations);

            // 평가 이후 금액 차감을 비동기로 처리합니다.
            faceEvaluateService.updateRemainingBalanceAsync(evaluations);

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return new EvaluationResponse(Collections.singletonList("Error: " + e.getMessage()));
        }
    }

    static class EvaluationResponse {
        private List<String> results;

        public EvaluationResponse(List<String> results) {
            this.results = results;
        }

        public List<String> getResults() {
            return results;
        }

        public void setResults(List<String> results) {
            this.results = results;
        }
    }

    @GetMapping("/api/faceEvaluate/balance")
    @ResponseBody
    public FaceEvaluateDTO getRemainingBalance() {
        BigDecimal balance = faceEvaluateService.getCurrentBalance();
        return new FaceEvaluateDTO(balance);
    }
}
