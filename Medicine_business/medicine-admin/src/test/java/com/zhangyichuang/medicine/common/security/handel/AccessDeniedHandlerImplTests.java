package com.zhangyichuang.medicine.common.security.handel;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessDeniedHandlerImplTests {

    @Test
    void handle_ShouldWriteForbiddenResponseBody() throws Exception {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/system/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
        assertTrue(response.getContentAsString().contains("权限不足"));
    }
}
