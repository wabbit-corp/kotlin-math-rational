// SPDX-License-Identifier: AGPL-3.0-or-later

package one.wabbit.math

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class RoundMode {
    FLOOR,
    CEILING,
    TRUNC,
    HALF_UP,
    HALF_DOWN,
    HALF_EVEN,
}

private fun parseBigInteger(text: String): BigInteger =
    BigInteger.parseString(text.trim().removePrefix("+"))

private fun bigInt(value: Long): BigInteger = parseBigInteger(value.toString())

private val BIG_INT_ZERO = bigInt(0)
private val BIG_INT_ONE = bigInt(1)
private val BIG_INT_TWO = bigInt(2)
private val BIG_INT_MINUS_ONE = bigInt(-1)

private fun signAsBigInteger(sign: Int): BigInteger =
    when (sign) {
        -1 -> BIG_INT_MINUS_ONE
        0 -> BIG_INT_ZERO
        1 -> BIG_INT_ONE
        else -> throw IllegalArgumentException("unexpected sign: $sign")
    }

private fun gcd(a: BigInteger, b: BigInteger): BigInteger {
    var x = a.abs()
    var y = b.abs()
    while (y != BIG_INT_ZERO) {
        val remainder = x % y
        x = y
        y = remainder.abs()
    }
    return x
}

private fun pow(base: BigInteger, exponent: Int): BigInteger {
    require(exponent >= 0) { "exponent must be >= 0" }

    var result = BIG_INT_ONE
    var factor = base
    var power = exponent

    while (power > 0) {
        if ((power and 1) == 1) {
            result *= factor
        }
        if (power > 1) {
            factor *= factor
        }
        power = power shr 1
    }

    return result
}

private class BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigInteger) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigInteger = parseBigInteger(decoder.decodeString())
}

@Serializable
@ConsistentCopyVisibility
data class Rational
private constructor(
    val numerator: @Serializable(with = BigIntegerSerializer::class) BigInteger,
    val denominator: @Serializable(with = BigIntegerSerializer::class) BigInteger,
) : Number(), Comparable<Rational> {
    init {
        require(denominator != BIG_INT_ZERO) { "denominator is zero" }
    }

    val isZero: Boolean
        get() = numerator == BIG_INT_ZERO

    val isOne: Boolean
        get() = numerator == BIG_INT_ONE && denominator == BIG_INT_ONE

    val isMinusOne: Boolean
        get() = numerator == BIG_INT_MINUS_ONE && denominator == BIG_INT_ONE

    val isPositive: Boolean
        get() = numerator.signum() > 0

    val isNegative: Boolean
        get() = numerator.signum() < 0

    val isInteger: Boolean
        get() = denominator == BIG_INT_ONE

    val isProper: Boolean
        get() = numerator.abs() < denominator

    val isImproper: Boolean
        get() = numerator.abs() >= denominator

    val isUnit: Boolean
        get() = numerator.abs() == BIG_INT_ONE

    override fun toString(): String =
        if (denominator == BIG_INT_ONE) {
            numerator.toString()
        } else {
            "$numerator/$denominator"
        }

    override fun toDouble(): kotlin.Double = numerator.toString().toDouble() / denominator.toString().toDouble()

    override fun toFloat(): Float = toDouble().toFloat()

    override fun toLong(): Long = toDouble().toLong()

    override fun toInt(): Int = toDouble().toInt()

    override fun toByte(): Byte = toDouble().toInt().toByte()

    override fun toShort(): Short = toDouble().toInt().toShort()

    operator fun plus(other: Rational): Rational =
        Rational.from(
            numerator * other.denominator + other.numerator * denominator,
            denominator * other.denominator,
        )

    operator fun plus(other: Int): Rational = this + Rational.from(other)

    operator fun plus(other: Long): Rational = this + Rational.from(other)

    operator fun minus(other: Rational): Rational =
        Rational.from(
            numerator * other.denominator - other.numerator * denominator,
            denominator * other.denominator,
        )

    operator fun minus(other: Int): Rational = this - Rational.from(other)

    operator fun minus(other: Long): Rational = this - Rational.from(other)

    operator fun times(other: Rational): Rational =
        Rational.from(numerator * other.numerator, denominator * other.denominator)

    operator fun times(other: Int): Rational = this * Rational.from(other)

    operator fun times(other: Long): Rational = this * Rational.from(other)

    operator fun div(other: Rational): Rational =
        Rational.from(numerator * other.denominator, denominator * other.numerator)

    operator fun div(other: Int): Rational = this / Rational.from(other)

    operator fun div(other: Long): Rational = this / Rational.from(other)

    operator fun unaryMinus(): Rational = Rational(-numerator, denominator)

    override operator fun compareTo(other: Rational): Int {
        val signA = numerator.signum()
        val signB = other.numerator.signum()
        if (signA != signB) {
            return signA - signB
        }
        if (numerator == BIG_INT_ZERO && other.numerator == BIG_INT_ZERO) {
            return 0
        }
        return (numerator * other.denominator).compareTo(other.numerator * denominator)
    }

    /** mod operation: a mod b = a - floor(a/b) * b */
    fun mod(other: Rational): Rational {
        require(other != zero) { "Cannot mod by zero" }
        val div = this / other
        // Use floor rounding:
        val floorDiv = div.round(RoundMode.FLOOR)
        return this - (floorDiv * other)
    }

    operator fun rem(other: Rational): Rational = mod(other)

    fun round(mode: RoundMode): Rational {
        val q = numerator / denominator
        val r = numerator % denominator

        if (r == BIG_INT_ZERO) {
            return Rational(q, BIG_INT_ONE)
        }

        return when (mode) {
            RoundMode.FLOOR -> {
                if (numerator.signum() < 0) {
                    Rational(q - BIG_INT_ONE, BIG_INT_ONE)
                } else {
                    Rational(q, BIG_INT_ONE)
                }
            }
            RoundMode.CEILING -> {
                if (numerator.signum() > 0) {
                    Rational(q + BIG_INT_ONE, BIG_INT_ONE)
                } else {
                    Rational(q, BIG_INT_ONE)
                }
            }
            RoundMode.TRUNC -> {
                Rational(q, BIG_INT_ONE)
            }
            RoundMode.HALF_UP,
            RoundMode.HALF_DOWN,
            RoundMode.HALF_EVEN -> {
                val absR = r.abs() * BIG_INT_TWO
                val cmp = absR.compareTo(denominator)

                when {
                    cmp < 0 -> Rational(q, BIG_INT_ONE)
                    cmp > 0 -> Rational(q + signAsBigInteger(r.signum()), BIG_INT_ONE)
                    else -> {
                        when (mode) {
                            RoundMode.HALF_UP -> Rational(q + signAsBigInteger(r.signum()), BIG_INT_ONE)
                            RoundMode.HALF_DOWN -> Rational(q, BIG_INT_ONE)
                            RoundMode.HALF_EVEN -> {
                                if (q % BIG_INT_TWO == BIG_INT_ZERO) {
                                    Rational(q, BIG_INT_ONE)
                                } else {
                                    Rational(q + signAsBigInteger(r.signum()), BIG_INT_ONE)
                                }
                            }
                            else -> throw IllegalStateException("Unexpected round mode")
                        }
                    }
                }
            }
        }
    }

    /** Truncate (toward zero). */
    fun truncRational(): Rational = round(RoundMode.TRUNC)

    /** Floor function (always rounding down, even for negative numbers). */
    fun floorRational(): Rational = round(RoundMode.FLOOR)

    /** Ceiling function (always rounding up). */
    fun ceilRational(): Rational = round(RoundMode.CEILING)

    /** Round half up. */
    fun roundHalfUp(): Rational = round(RoundMode.HALF_UP)

    /** Round half down. */
    fun roundHalfDown(): Rational = round(RoundMode.HALF_DOWN)

    /** Round half to even. */
    fun roundHalfEven(): Rational = round(RoundMode.HALF_EVEN)

    fun abs(): Rational =
        if (numerator < BIG_INT_ZERO) {
            Rational(-numerator, denominator)
        } else {
            this
        }

    fun reciprocal(): Rational = Rational(denominator, numerator)

    fun floor(): Rational = floorRational()

    fun ceil(): Rational = ceilRational()

    fun round(): Rational = roundHalfUp()

    fun signum(): Int = numerator.signum()

    fun pow(n: Int): Rational =
        when {
            n > 0 -> Rational.from(pow(numerator, n), pow(denominator, n))
            n == 0 -> one
            else -> Rational.from(pow(denominator, -n), pow(numerator, -n))
        }

    companion object {
        val zero = Rational(BIG_INT_ZERO, BIG_INT_ONE)
        val one = Rational(BIG_INT_ONE, BIG_INT_ONE)
        val minusOne = Rational(BIG_INT_MINUS_ONE, BIG_INT_ONE)
        val half = Rational(BIG_INT_ONE, BIG_INT_TWO)

        fun parse(s: String): Rational {
            val parts = s.split("/")
            if (parts.size == 1) {
                return Rational(parseBigInteger(parts[0]), BIG_INT_ONE)
            } else if (parts.size == 2) {
                return Rational.from(parseBigInteger(parts[0]), parseBigInteger(parts[1]))
            } else {
                throw IllegalArgumentException("invalid rational: $s")
            }
        }

        operator fun invoke(numerator: BigInteger, denominator: BigInteger): Rational =
            from(numerator, denominator)

        fun from(numerator: BigInteger, denominator: BigInteger): Rational {
            val divisor = gcd(numerator, denominator)
            val n = numerator / divisor
            val d = denominator / divisor
            if (d < BIG_INT_ZERO) {
                return Rational(-n, -d)
            } else {
                return Rational(n, d)
            }
        }

        fun from(numerator: Long, denominator: Long): Rational =
            from(bigInt(numerator), bigInt(denominator))

        fun from(numerator: Int, denominator: Int): Rational =
            from(numerator.toLong(), denominator.toLong())

        fun from(numerator: BigInteger): Rational = Rational(numerator, BIG_INT_ONE)

        fun from(numerator: Long): Rational = Rational(bigInt(numerator), BIG_INT_ONE)

        fun from(numerator: Int): Rational = Rational.from(numerator.toLong())
    }
}
