package com.house.houseviewing.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
AU: 로그인, 권한 토큰 -> 보안 관련
VP: 사용자가 잘못 보낸 값 -> 입력값 오류
DB: 데이터 충돌, 무결성 깨짐
ER: 서버가 터짐
NF: 리소스 없음

400: 입력값 오류
401: 신원 인증 실패
409: 이미 있던 데이터와 충돌(중복)
 */

@Getter @AllArgsConstructor
public enum ExceptionCode {

    // AU
    UNAUTHORIZED("AU001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN("AU002", HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."),
    FORBIDDEN("AU003", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    LOGIN_FAILED("AU004",HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    FREE_DIAGNOSIS_ALREADY_USED("AU005", HttpStatus.FORBIDDEN, "이미 무료 진단을 사용하셨습니다. 결제를 진행해주세요"),
    INVALID_HEADER("AU005", HttpStatus.UNAUTHORIZED, "올바른 Authorization 헤더가 아닙니다."),
    PASSWORD_RESET_NOT_ALLOWED("AU006", HttpStatus.FORBIDDEN, "비밀번호 재설정 권한이 없습니다."),

    // VP
    FIND_LOGIN_ID_FAILED("VP001",HttpStatus.BAD_REQUEST, "이메일 또는 아이디가 틀렸습니다."),
    VERIFY_PASSWORD_FAILED("VP002", HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다."),
    VERIFY_FILE_FAILED("VP003", HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다"),
    INVALID_PDF_REQUEST("VP004", HttpStatus.BAD_REQUEST, "PDF 생성 요청이 올바르지 않습니다."),

    // ER
    FILE_SAVE_FAILED("ER001", HttpStatus.BAD_REQUEST, "파일 저장에 실패했습니다."),
    ANALYSIS_FAILED("ER002", HttpStatus.INTERNAL_SERVER_ERROR, "등기부 분석에 실패했습니다."),
    PDF_SAVE_FAILED("ER003", HttpStatus.BAD_REQUEST, "PDF 저장에 실패했습니다."),
    S3_UPLOAD_FAILED("ER004", HttpStatus.INTERNAL_SERVER_ERROR, "S3 서버로의 파일 업로드에 실패했습니다."),
    PDF_GENERATION_FAILED("ER005", HttpStatus.BAD_GATEWAY, "PDF 생성 서버 호출에 실패했습니다."),

    // NF
    USER_NOT_FOUND("NF001", HttpStatus.NOT_FOUND, "해당 사용자는 존재하지 않습니다."),
    ADDRESS_NOT_FOUND("NF002", HttpStatus.NOT_FOUND, "주소를 다시 확인해주세요."),
    HOUSE_NOT_FOUND("NF003", HttpStatus.NOT_FOUND, "해당 집은 등록되지 않았습니다."),
    CONTRACT_NOT_FOUND("NF004", HttpStatus.NOT_FOUND, "해당 계약은 등록되지 않았습니다"),
    SNAPSHOT_NOT_FOUND("NF005", HttpStatus.NOT_FOUND, "해당 스냅샷을 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_FOUND("NF006", HttpStatus.NOT_FOUND, "해당 구독권을 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND("NF007", HttpStatus.NOT_FOUND, "해당 분석기록을 찾을 수 없습니다."),
    MOCK_NOT_FOUND("NF008", HttpStatus.NOT_FOUND, "MOCK 파일을 찾을 수 없습니다."),

    //DB
    DUPLICATE_LOGIN_ID("DB001",HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL("DB002", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_RESOURCE("DB003", HttpStatus.CONFLICT, "이미 사용 중입니다."),
    ALREADY_REGISTERED_CONTRACT("DB004", HttpStatus.CONFLICT, "이미 등록된 계약이 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
