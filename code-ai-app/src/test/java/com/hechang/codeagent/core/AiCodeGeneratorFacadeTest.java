package com.hechang.codeagent.core;

import com.hechang.codeagent.core.builder.VueProjectBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCodeGeneratorFacadeTest {

    @Test
    void buildsExistingVueProjectWhenTheModelStreamBecomesInactive() {
        VueProjectBuilder vueProjectBuilder = mock(VueProjectBuilder.class);
        when(vueProjectBuilder.buildProject(endsWith("vue_project_9"))).thenReturn(true);
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        ReflectionTestUtils.setField(facade, "vueProjectBuilder", vueProjectBuilder);

        facade.buildVueProjectAfterInactivity(9L).block();

        verify(vueProjectBuilder).buildProject(endsWith("vue_project_9"));
    }
}
