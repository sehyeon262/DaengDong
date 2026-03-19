package com.e108.be.domain.record.controller;

import com.e108.be.domain.record.dto.response.CalendarResponse;
import com.e108.be.domain.record.dto.response.DailyRecordResponse;
import com.e108.be.domain.record.dto.response.RecordDetailResponse;
import com.e108.be.domain.record.service.RecordService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/calendar")
    public ResTemplate<CalendarResponse> getCalendar(
            @RequestParam Long dogId,
            @RequestParam int year,
            @RequestParam int month) {
        CalendarResponse response = recordService.getCalendar(dogId, year, month);
        return ResTemplate.success(HttpStatus.OK, "캘린더 조회 성공", response);
    }

    @GetMapping("/daily")
    public ResTemplate<DailyRecordResponse> getDailyRecords(
            @RequestParam Long dogId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyRecordResponse response = recordService.getDailyRecords(dogId, date);
        return ResTemplate.success(HttpStatus.OK, "산책 목록 조회 성공", response);
    }

    @GetMapping("/{recordId}")
    public ResTemplate<RecordDetailResponse> getRecordDetail(@PathVariable Long recordId) {
        RecordDetailResponse response = recordService.getRecordDetail(recordId);
        return ResTemplate.success(HttpStatus.OK, "산책 상세 조회 성공", response);
    }
}
