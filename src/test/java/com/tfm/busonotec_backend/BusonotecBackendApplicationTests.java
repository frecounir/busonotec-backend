package com.tfm.busonotec_backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BusonotecBackendApplicationTests {

	@Test
	void applicationClassCanBeLoadedWithoutStartingSpring() {
		assertDoesNotThrow(() -> Class.forName(BusonotecBackendApplication.class.getName()));
	}

}
