package com.e108.be.domain.safety.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class RiskReportNotFoundException extends NotFoundGroupException {

    public RiskReportNotFoundException() {
        super("삭제할 위험 구역을 찾을 수 없습니다.");
    }
}
