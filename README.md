# Kotlin Math Rational Library

This project is a Kotlin-based library that provides robust support for rational numbers, facilitating mathematical operations with fractions in a precise and efficient manner.

## Overview

The primary goal of this library is to offer an easy-to-use and highly precise representation of rational numbers, allowing users to perform arithmetic operations, comparisons, and conversions seamlessly. The library also supports serialization via Kotlinx Serialization.

### Key Components

#### Rational Class
- **Mathematical Representation**: Models rational numbers using `BigInteger` for both numerator and denominator, ensuring high precision and vast range.
- **Operations**: Supports arithmetic operations such as addition, subtraction, multiplication, and division, along with comparison and conversion to various numeric types.
- **Serialization**: Annotated with `@Serializable`, enabling serialization/deserialization of rational number instances.
- **Normalization**: Always stores numbers in their simplest form by dividing the numerator and denominator by their greatest common divisor.
- **Utility Methods**: Includes utility methods like `toString`, which provide standard and readable string representations of rational numbers.

## Installation

Add the following dependency to your project:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.wabbit-corp:kotlin-math-rational:1.0.0")
}
```

## Usage

### Key Usage Examples

1. **Creating Rational Numbers**:
   - Use `Rational.from()` to create instances. Example:
     ```kotlin
     val half = Rational.from(1, 2) // Represents 1/2
     val negative = Rational.from(-3, 4) // Represents -3/4
     ```

2. **Arithmetic Operations**:
   - Demonstrating operations:
     ```kotlin
     val sum = Rational.from(1, 2) + Rational.from(1, 3)
     val product = Rational.from(2) * Rational.from(5, 2)
     ```

3. **Comparison and Sorting**:
   - Examples using comparable:
     ```kotlin
     val isEqual = Rational.from(1, 2) == Rational.from(2, 4)
     val comparison = Rational.from(3, 4) < Rational.from(4, 5)
     ```

4. **Serialization**:
   - Serializing to string:
     ```kotlin
     val jsonString = Json.encodeToString(Rational(BigInteger("3"), BigInteger("4")))
     ```
   - Deserializing from string:
     ```kotlin
     val rationalObj = Json.decodeFromString<Rational>(jsonString)
     ```

These examples cover the library's primary functionalities and demonstrate its integration with Kotlin serialization.

## Licensing

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) for open source use.

For commercial use, please contact Wabbit Consulting Corporation (at wabbit@wabbit.one) for licensing terms.

## Contributing

Before we can accept your contributions, we kindly ask you to agree to our Contributor License Agreement (CLA).
