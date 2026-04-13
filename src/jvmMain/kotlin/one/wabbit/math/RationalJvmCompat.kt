// SPDX-License-Identifier: AGPL-3.0-or-later

package one.wabbit.math

private fun java.math.BigInteger.toKmpBigInteger(): com.ionspin.kotlin.bignum.integer.BigInteger =
    com.ionspin.kotlin.bignum.integer.BigInteger.parseString(toString())

fun Rational.Companion.from(
    numerator: java.math.BigInteger,
    denominator: java.math.BigInteger,
): Rational = from(numerator.toKmpBigInteger(), denominator.toKmpBigInteger())

fun Rational.Companion.from(numerator: java.math.BigInteger): Rational =
    from(numerator.toKmpBigInteger())

operator fun Rational.Companion.invoke(
    numerator: java.math.BigInteger,
    denominator: java.math.BigInteger,
): Rational = from(numerator, denominator)
