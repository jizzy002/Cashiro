package com.ritesh.cashiro.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelLoadPolicyTest {

    @Test
    fun `an uninitialized engine must be initialized`() {
        assertTrue(
            ModelLoadPolicy.needsInitialization(
                isInitialized = false,
                loadedPath = null,
                selectedPath = "/models/qwen.task"
            )
        )
    }

    @Test
    fun `the loaded model is reused when it is still selected`() {
        assertFalse(
            ModelLoadPolicy.needsInitialization(
                isInitialized = true,
                loadedPath = "/models/qwen.task",
                selectedPath = "/models/qwen.task"
            )
        )
    }

    @Test
    fun `a different selected model forces a reload`() {
        assertTrue(
            ModelLoadPolicy.needsInitialization(
                isInitialized = true,
                loadedPath = "/models/qwen.task",
                selectedPath = "/models/smollm.task"
            )
        )
    }

    @Test
    fun `an initialized engine with an unknown path is reloaded`() {
        assertTrue(
            ModelLoadPolicy.needsInitialization(
                isInitialized = true,
                loadedPath = null,
                selectedPath = "/models/qwen.task"
            )
        )
    }
}
