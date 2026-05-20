package com.devsignal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class DevSignalApplicationTests {

	@Test
	void applicationIsAnnotatedForBootstrapping() {
		assertNotNull(DevSignalApplication.class.getAnnotation(SpringBootApplication.class));
	}

}
