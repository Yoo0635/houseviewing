package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.controller.AnalysisQueryController;
import com.house.houseviewing.api.query.dto.AnalysisHistoryPageResponse;
import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisQueryControllerTest {

    private final AnalysisQueryService analysisQueryService = mock(AnalysisQueryService.class);
    private final CustomUserDetails userDetails = new CustomUserDetails(UserEntity.builder()
            .id(1L)
            .name("사용자")
            .email("user@test.com")
            .loginId("user")
            .password("password")
            .build());
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AnalysisQueryController(analysisQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(userDetails))
            .build();

    @Test
    void 목록_조회_offset_기본값은_0이다() throws Exception {
        given(analysisQueryService.getAnalyses(1L, 0L, null))
                .willReturn(new AnalysisHistoryPageResponse(List.of(), null, false));

        mockMvc.perform(get("/analyses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(false));

        then(analysisQueryService).should().getAnalyses(1L, 0L, null);
    }

    @Test
    void 음수_offset은_400이다() throws Exception {
        willThrow(new AppException(ExceptionCode.INVALID_PDF_REQUEST, "offset은 0 이상 10000 이하이어야 합니다."))
                .given(analysisQueryService).getAnalyses(1L, -1L, null);

        mockMvc.perform(get("/analyses").param("offset", "-1"))
                .andExpect(status().isBadRequest());
    }

    private record TestAuthenticationPrincipalResolver(CustomUserDetails userDetails)
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return userDetails;
        }
    }
}
