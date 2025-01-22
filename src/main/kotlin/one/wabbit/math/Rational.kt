package one.wabbit.math

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger
import kotlin.time.times

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

    operator fun unaryMinus(): Rational {
        return Rational(-numerator, denominator)
    }

    override operator fun compareTo(other: Rational): Int {
        return (numerator * other.denominator).compareTo(other.numerator * denominator)
    }

    companion object {
        val zero     = Rational(BigInteger.ZERO, BigInteger.ONE)
        val one      = Rational(BigInteger.ONE, BigInteger.ONE)
        val minusOne = Rational(BigInteger.ONE.negate(), BigInteger.ONE)
        val half     = Rational(BigInteger.ONE, BigInteger.valueOf(2))

        fun parse(s: String): Rational {
            val parts = s.split("/")
            if (parts.size == 1) {
                return Rational(BigInteger(parts[0]), BigInteger.ONE)
            } else if (parts.size == 2) {
                return Rational.from(BigInteger(parts[0]), BigInteger(parts[1]))
            } else {
                throw IllegalArgumentException("invalid rational: $s")
            }
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
