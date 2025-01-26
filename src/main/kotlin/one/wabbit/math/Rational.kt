package one.wabbit.math

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

enum class RoundMode {
    FLOOR,
    CEILING,
    TRUNC,
    HALF_UP,
    HALF_DOWN,
    HALF_EVEN
}

private class BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigInteger) {
        encoder.encodeString(value.toString())
    }
    override fun deserialize(decoder: Decoder): BigInteger {
        return BigInteger(decoder.decodeString())
    }
}

@Serializable
data class Rational private constructor (
    val numerator: @Serializable(with=BigIntegerSerializer::class) BigInteger,
    val denominator: @Serializable(with=BigIntegerSerializer::class) BigInteger
) : Comparable<Rational>, Number(), java.io.Serializable {
    init {
        require(denominator != BigInteger.ZERO) { "denominator is zero" }
    }

    val isZero: Boolean get () =
        numerator == BigInteger.ZERO
    val isOne: Boolean get() =
        numerator == BigInteger.ONE && denominator == BigInteger.ONE
    val isMinusOne: Boolean get() =
        numerator == BigInteger.ONE.negate() && denominator == BigInteger.ONE
    val isPositive: Boolean get() =
        numerator.signum() > 0
    val isNegative: Boolean get() =
        numerator.signum() < 0
    val isInteger: Boolean get() =
        denominator == BigInteger.ONE
    val isProper: Boolean get() =
        numerator.abs() < denominator
    val isImproper: Boolean get() =
        numerator.abs() >= denominator
    val isUnit: Boolean get() =
        numerator.abs() == BigInteger.ONE

    override fun toString(): String {
        return if (denominator == BigInteger.ONE) {
            numerator.toString()
        } else {
            "$numerator/$denominator"
        }
    }

    override fun toDouble(): kotlin.Double {
        return numerator.toDouble() / denominator.toDouble()
    }
    override fun toFloat(): Float = toDouble().toFloat()
    override fun toLong(): Long = toDouble().toLong()
    override fun toInt(): Int = toDouble().toInt()
    override fun toByte(): Byte = toDouble().toInt().toByte()
    override fun toShort(): Short = toDouble().toInt().toShort()

    operator fun plus(other: Rational): Rational {
        return Rational.from(
            numerator * other.denominator + other.numerator * denominator,
            denominator * other.denominator
        )
    }
    operator fun plus(other: Int): Rational = this + Rational.from(other)
    operator fun plus(other: Long): Rational = this + Rational.from(other)

    operator fun minus(other: Rational): Rational {
        return Rational.from(
            numerator * other.denominator - other.numerator * denominator,
            denominator * other.denominator
        )
    }
    operator fun minus(other: Int): Rational = this - Rational.from(other)
    operator fun minus(other: Long): Rational = this - Rational.from(other)

    operator fun times(other: Rational): Rational {
        return Rational.from(
            numerator * other.numerator,
            denominator * other.denominator
        )
    }
    operator fun times(other: Int): Rational = this * Rational.from(other)
    operator fun times(other: Long): Rational = this * Rational.from(other)

    operator fun div(other: Rational): Rational {
        return Rational.from(
            numerator * other.denominator,
            denominator * other.numerator
        )
    }
    operator fun div(other: Int): Rational = this / Rational.from(other)
    operator fun div(other: Long): Rational = this / Rational.from(other)

    operator fun unaryMinus(): Rational = Rational(-numerator, denominator)

    override operator fun compareTo(other: Rational): Int {
        // 1) sign check
        val signA = numerator.signum()
        val signB = other.numerator.signum()
        if (signA != signB) {
            return signA - signB // e.g. -1 if signA < signB
        }
        // 2) both zero?
        if (numerator == BigInteger.ZERO && other.numerator == BigInteger.ZERO) {
            return 0
        }
        // 3) approximate exponent check
        val expA = numerator.abs().bitLength() - denominator.bitLength()
        val expB = other.numerator.abs().bitLength() - other.denominator.bitLength()
        if (signA > 0) {
            if (expA > expB + 1) return 1
            if (expA < expB - 1) return -1
        } else {
            // both negative
            if (expA > expB + 1) return -1
            if (expA < expB - 1) return 1
        }
        // 4) precise cross multiplication
        val cross1 = numerator.multiply(other.denominator)
        val cross2 = other.numerator.multiply(denominator)
        return cross1.compareTo(cross2)
    }

    /**
     * mod operation:
     * a mod b = a - floor(a/b) * b
     */
    fun mod(other: Rational): Rational {
        require(other != zero) { "Cannot mod by zero" }
        val div = this / other
        // Use floor rounding:
        val floorDiv = div.round(RoundMode.FLOOR)
        return this - (floorDiv * other)
    }

    operator fun rem(other: Rational): Rational = mod(other)

    fun round(mode: RoundMode): Rational {
        val (q, r) = numerator.divideAndRemainder(denominator)

        if (r == BigInteger.ZERO) {
            // It's already an integer
            return Rational(q, BigInteger.ONE)
        }

        return when (mode) {
            RoundMode.FLOOR -> {
                // floor = q unless remainder < 0 => then q - 1
                if (numerator.signum() < 0) {
                    // negative fraction => floor is q - 1
                    Rational(q - BigInteger.ONE, BigInteger.ONE)
                } else {
                    // positive fraction => floor is q
                    Rational(q, BigInteger.ONE)
                }
            }
            RoundMode.CEILING -> {
                // ceiling = q unless remainder > 0 => then q + 1
                if (numerator.signum() > 0) {
                    Rational(q + BigInteger.ONE, BigInteger.ONE)
                } else {
                    Rational(q, BigInteger.ONE)
                }
            }
            RoundMode.TRUNC -> {
                // truncate toward 0 => just q
                Rational(q, BigInteger.ONE)
            }
            RoundMode.HALF_UP, RoundMode.HALF_DOWN, RoundMode.HALF_EVEN -> {
                // We must decide how to handle the "exactly half" case
                // Compare 2*|r| vs denominator
                val absR = r.abs().shiftLeft(1)   // multiply remainder by 2
                val cmp = absR.compareTo(denominator)

                // if cmp < 0 => fraction part < 0.5 => q
                // if cmp > 0 => fraction part > 0.5 => q + sign
                // if cmp = 0 => fraction part == 0.5 => depends on half rule
                when {
                    cmp < 0 -> {
                        // < 0.5 => q
                        Rational(q, BigInteger.ONE)
                    }
                    cmp > 0 -> {
                        // > 0.5 => q + sign
                        Rational(q + r.signum().toBigInteger(), BigInteger.ONE)
                    }
                    else -> {
                        // exactly 0.5
                        when (mode) {
                            RoundMode.HALF_UP -> {
                                // away from zero
                                Rational(q + r.signum().toBigInteger(), BigInteger.ONE)
                            }
                            RoundMode.HALF_DOWN -> {
                                // toward zero
                                Rational(q, BigInteger.ONE)
                            }
                            RoundMode.HALF_EVEN -> {
                                // choose nearest even integer
                                if (q.and(BigInteger.ONE) == BigInteger.ZERO) {
                                    // q is even
                                    Rational(q, BigInteger.ONE)
                                } else {
                                    // q is odd
                                    Rational(q + r.signum().toBigInteger(), BigInteger.ONE)
                                }
                            }
                            else -> throw IllegalStateException("Unexpected round mode")
                        }
                    }
                }
            }
        }
    }

    /**
     * Truncate (toward zero).
     */
    fun truncRational(): Rational = round(RoundMode.TRUNC)

    /**
     * Floor function (always rounding down, even for negative numbers).
     */
    fun floorRational(): Rational = round(RoundMode.FLOOR)

    /**
     * Ceiling function (always rounding up).
     */
    fun ceilRational(): Rational = round(RoundMode.CEILING)

    /**
     * Round half up.
     */
    fun roundHalfUp(): Rational = round(RoundMode.HALF_UP)

    /**
     * Round half down.
     */
    fun roundHalfDown(): Rational = round(RoundMode.HALF_DOWN)

    /**
     * Round half to even.
     */
    fun roundHalfEven(): Rational = round(RoundMode.HALF_EVEN)

    fun abs(): Rational {
        return if (numerator < BigInteger.ZERO) {
            Rational(-numerator, denominator)
        } else {
            this
        }
    }

    fun reciprocal(): Rational {
        return Rational(denominator, numerator)
    }

    fun floor(): Rational {
        return Rational(numerator / denominator, BigInteger.ONE)
    }

    fun ceil(): Rational {
        return Rational((numerator + denominator - BigInteger.ONE) / denominator, BigInteger.ONE)
    }

    fun round(): Rational {
        return Rational((numerator + denominator / BigInteger.valueOf(2)) / denominator, BigInteger.ONE)
    }

    fun signum(): Int {
        return numerator.signum()
    }

    fun pow(n: Int): Rational {
        return when {
            n > 0 -> Rational.from(numerator.pow(n), denominator.pow(n))
            n == 0 -> one
            else -> Rational.from(denominator.pow(-n), numerator.pow(-n)) // negative exponent
        }
    }

    companion object {
        val zero     = Rational(BigInteger.ZERO, BigInteger.ONE)
        val one      = Rational(BigInteger.ONE, BigInteger.ONE)
        val minusOne = Rational(BigInteger.ONE.negate(), BigInteger.ONE)
        val half     = Rational(BigInteger.ONE, BigInteger.valueOf(2))

        fun parse(s: String): Rational {
            val parts = s.split("/")
            if (parts.size == 1) {
                return Rational(BigInteger(parts[0].trim()), BigInteger.ONE)
            } else if (parts.size == 2) {
                return Rational.from(BigInteger(parts[0].trim()), BigInteger(parts[1].trim()))
            } else {
                throw IllegalArgumentException("invalid rational: $s")
            }
        }

        operator fun invoke(numerator: BigInteger, denominator: BigInteger): Rational {
            return from(numerator, denominator)
        }

        fun from(numerator: BigInteger, denominator: BigInteger): Rational {
            // GCD
            val gcd = numerator.gcd(denominator)
            val n = numerator / gcd
            val d = denominator / gcd
            if (d < BigInteger.ZERO) {
                return Rational(-n, -d)
            } else {
                return Rational(n, d)
            }
        }
        fun from(numerator: Long, denominator: Long): Rational =
            from(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))
        fun from(numerator: Int, denominator: Int): Rational =
            from(BigInteger.valueOf(numerator.toLong()), BigInteger.valueOf(denominator.toLong()))

        fun from(numerator: BigInteger): Rational = Rational(numerator, BigInteger.ONE)
        fun from(numerator: Long): Rational = Rational(BigInteger.valueOf(numerator), BigInteger.ONE)
        fun from(numerator: Int): Rational = Rational(BigInteger.valueOf(numerator.toLong()), BigInteger.ONE)
    }
}
