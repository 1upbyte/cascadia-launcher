package app.cascadia.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RailCurveTest {
    @Test fun activeLetterIsAtPeak() {
        assertEquals(1f, railCurveWeight(0f), 0f)
    }

    @Test fun peakMovesContinuouslyBetweenLetters() {
        assertEquals(railCurveWeight(-9.5f, 70f), railCurveWeight(9.5f, 70f), 0f)
        assertTrue(railCurveWeight(0f, 70f) > railCurveWeight(9.5f, 70f))
    }

    @Test fun longerRailAndDeeperPullBendMoreNeighbors() {
        val spread = railCurveSpread(420f, 64f)
        assertTrue(railCurveSpread(420f, 120f) > spread)
        assertTrue(railCurveSpread(520f, 64f) > spread)
        assertTrue(railCurveWeight(19f, spread) > 0.9f)
        assertTrue(railCurveWeight(190f, spread) < 0.05f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroSpreadIsRejected() {
        railCurveWeight(0f, 0f)
    }

    @Test fun neighborsTaperSymmetricallyTowardEdge() {
        assertTrue(railCurveWeight(1f) < railCurveWeight(0f))
        assertTrue(railCurveWeight(2f) < railCurveWeight(1f))
        assertTrue(railCurveWeight(3f) < railCurveWeight(2f))
        assertTrue(railCurveWeight(4f) < 0.01f)
        assertEquals(railCurveWeight(2f), railCurveWeight(-2f), 0f)
    }
}
