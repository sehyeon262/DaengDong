package com.e108.be.domain.diary.controller;

import com.e108.be.domain.diary.dto.response.DiaryResponse;
import com.e108.be.domain.diary.service.DiaryService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping
    public ResTemplate<DiaryResponse> getDiary(@RequestParam Long walkId) {
        DiaryResponse response = diaryService.getDiary(walkId);
        return ResTemplate.success(HttpStatus.OK, "일기 조회 성공", response);
    }

    /**
     * 기존 산책 데이터에 대해 수동으로 일기 생성 요청
     * POST /api/v1/diaries/generate?walkId=1
     */
    @PostMapping("/generate")
    public ResTemplate<Void> generateDiary(@RequestParam Long walkId) {
        diaryService.generateForExistingWalk(walkId);
        return ResTemplate.success(HttpStatus.OK, "일기 생성 요청 완료");
    }
}
