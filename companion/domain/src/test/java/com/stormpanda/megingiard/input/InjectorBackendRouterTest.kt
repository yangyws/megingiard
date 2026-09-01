package com.stormpanda.megingiard.input

import com.stormpanda.megingiard.privd.PrivdClient
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InjectorBackendRouterTest {
    @Before
    fun setUp() {
        PrivdClient.setStateForTesting(null)
    }

    @After
    fun tearDown() {
        PrivdClient.setStateForTesting(null)
    }
    @Test
    fun testResolveBackendWhenDisconnectedReturnsFalse() {
        val router = InjectorBackendRouter("TestTag")
        val isPrivd = router.resolveBackend()
        assertFalse(isPrivd)
        assertFalse(router.isPrivd)
    }

    @Test
    fun testIsRunningDelegatesToFallbackWhenDisconnected() {
        val router = InjectorBackendRouter("TestTag")
        router.resolveBackend()

        var fallbackChecked = false
        val isRunning =
            router.isRunning {
                fallbackChecked = true
                true
            }

        assertTrue(fallbackChecked)
        assertTrue(isRunning)
    }

    @Test
    fun testMarkStoppedResetsActiveState() {
        val router = InjectorBackendRouter("TestTag")
        router.resolveBackend()
        router.markStopped()

        // When stopped, isRunning returns false immediately without checking fallback
        var fallbackChecked = false
        val isRunning =
            router.isRunning {
                fallbackChecked = true
                true
            }
        assertFalse(fallbackChecked)
        assertFalse(isRunning)
    }

    @Test
    fun testDispatchExecutesCorrectBranch() {
        val router = InjectorBackendRouter("TestTag")
        router.resolveBackend()

        var privdRan = false
        var shellRan = false

        router.dispatch(
            privdAction = { privdRan = true },
            shellAction = { shellRan = true },
        )

        assertFalse(privdRan)
        assertTrue(shellRan)
    }
}
