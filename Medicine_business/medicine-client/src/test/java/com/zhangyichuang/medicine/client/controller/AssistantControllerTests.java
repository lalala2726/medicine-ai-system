package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.service.MallOrderService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantControllerTests {

    @Mock
    private MallOrderService mallOrderService;

    @InjectMocks
    private AssistantController controller;

}
