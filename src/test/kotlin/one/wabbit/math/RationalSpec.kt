package one.wabbit.math
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RationalTests {
    @Test
    fun testCompareOptimization() {
        val huge1 = Rational.from(BigInteger("1").shiftLeft(5000), BigInteger.ONE)  // 2^5000
        val huge2 = Rational.from(BigInteger("1").shiftLeft(6000), BigInteger.ONE)  // 2^6000

        // obviously huge2 > huge1
        assertTrue(huge2 > huge1, "2^6000 should be > 2^5000")
    }

    @Test
    fun testMod() {
        val a = Rational.from(7, 3)   // 7/3
        val b = Rational.from(2, 1)   // 2
        // 7/3 = 2 + 1/3 => floor(7/3 / 2) = floor(7/6) = 1 => remainder is 7/3 - 1*2 = 7/3 - 2 = 1/3
        assertEquals(Rational.from(1, 3), a.rem(b))

        // negative test
        val c = Rational.from(-7, 3)  // -7/3
        // -7/3 / 2 = -7/6 => floor(-7/6) = -2 (since -7/6 ~ -1.166..., floor is -2)
        // remainder = -7/3 - (-2)*2 = -7/3 + 4 = (-7 + 12)/3 = 5/3
        assertEquals(Rational.from(5, 3), c.rem(b))
    }

    @Test
    fun testFloorCeil() {
        // Positive
        val r1 = Rational.from(7, 3) // 7/3 ~ 2.333...
        assertEquals("2", r1.floorRational().toString())
        assertEquals("3", r1.ceilRational().toString())

        // Negative
        val r2 = Rational.from(-7, 3) // -7/3 ~ -2.333...
        // floor => -3
        assertEquals("-3", r2.floorRational().toString())
        // ceil => -2
        assertEquals("-2", r2.ceilRational().toString())
    }

    @Test
    fun testRoundHalfUp() {
        // 2.4 => 2
        val a = Rational.from(24, 10)
        val b = Rational.from(25, 10)
        val c = Rational.from(26, 10)

        // 2.4 => < 0.5 => 2
        assertEquals("2", a.roundHalfUp().toString())
        // 2.5 => == 0.5 => round away from 0 => 3
        assertEquals("3", b.roundHalfUp().toString())
        // 2.6 => > 0.5 => 3
        assertEquals("3", c.roundHalfUp().toString())

        // Negative example: -2.5 => tie => round away => -3
        val neg = Rational.from(-25, 10)
        assertEquals("-3", neg.roundHalfUp().toString())
    }

    @Test
    fun testRoundHalfEven() {
        // 2.5 => tie => nearest even is 2
        val a = Rational.from(25, 10)
        assertEquals("2", a.roundHalfEven().toString())

        // 3.5 => tie => nearest even is 4? Actually, 3 is odd, so half to even => 4
        val b = Rational.from(35, 10)
        assertEquals("4", b.roundHalfEven().toString())

        // Negative tie test: -2.5 => q = -2 is even => stay at -2
        val c = Rational.from(-25, 10)
        assertEquals("-2", c.roundHalfEven().toString())

        // -3.5 => q = -3 is odd => go to -4
        val d = Rational.from(-35, 10)
        assertEquals("-4", d.roundHalfEven().toString())
    }
}