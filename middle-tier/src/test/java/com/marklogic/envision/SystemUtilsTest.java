package com.marklogic.envision;

import com.marklogic.envision.dataServices.SystemUtils;
import com.marklogic.grove.boot.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class SystemUtilsTest extends BaseTest {

	@BeforeEach
	void setUp() {
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();
	}

	@Test
	@WithMockUser
	void resetSystem() {
		SystemUtils.on(getFinalClient()).resetSystem();
	}
}
