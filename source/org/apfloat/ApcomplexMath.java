package org.apfloat;

import org.apfloat.spi.Util;

/**
 * Various mathematical functions for arbitrary precision complex numbers.
 *
 * @version 1.0.2
 * @author Mikko Tommila
 */

public class ApcomplexMath
{
    private ApcomplexMath()
    {
    }

    /**
     * Negative value.
     *
     * @param z The argument.
     *
     * @return <code>-z</code>.
     */

    public static Apcomplex negate(Apcomplex z)
    {
        return new Apcomplex(ApfloatMath.negate(z.real()), ApfloatMath.negate(z.imag()));
    }

    /**
     * Absolute value.
     *
     * @param z The argument.
     *
     * @return <code>sqrt(x<sup>2</sup> + y<sup>2</sup>)</code>, where <code>z = x + <i>i</i> y</code>.
     */

    public static Apfloat abs(Apcomplex z)
    {
        if (z.real().signum() == 0)
        {
             return ApfloatMath.abs(z.imag());
        }
        else if (z.imag().signum() == 0)
        {
             return ApfloatMath.abs(z.real());
        }
        else
        {
             return ApfloatMath.sqrt(norm(z));
        }
    }

    /**
     * Norm. Square of the magnitude.
     *
     * @param z The argument.
     *
     * @return <code>x<sup>2</sup> + y<sup>2</sup></code>, where <code>z = x + <i>i</i> y</code>.
     */

    public static Apfloat norm(Apcomplex z)
    {
        return ApfloatMath.multiplyAdd(z.real(), z.real(), z.imag(), z.imag());
    }

    /**
     * Angle of the complex vector in the complex plane.
     *
     * @param z The argument.
     *
     * @return <code>arctan(y / x)</code> from the appropriate branch, where <code>z = x + <i>i</i> y</code>.
     */

    public static Apfloat arg(Apcomplex z)
    {
        return ApfloatMath.atan2(z.imag(), z.real());
    }

    /**
     * Multiply by a power of the radix.
     *
     * @param z The argument.
     * @param scale The scaling factor.
     *
     * @return <code>z * z.radix()<sup>scale</sup></code>.
     */

    public static Apcomplex scale(Apcomplex z, long scale)
        throws ApfloatRuntimeException
    {
        return new Apcomplex(ApfloatMath.scale(z.real(), scale),
                             ApfloatMath.scale(z.imag(), scale));
    }

    /**
     * Integer power.
     *
     * @param z Base of the power operator.
     * @param n Exponent of the power operator.
     *
     * @return <code>z</code> to the <code>n</code>:th power, that is <code>z<sup>n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>z</code> and <code>n</code> are zero.
     */

    public static Apcomplex pow(Apcomplex z, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (z.real().signum() == 0 && z.imag().signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apcomplex(new Apfloat(1, Apfloat.INFINITE, z.radix()));
        }
        else if (n < 0)
        {
            z = Apcomplex.ONE.divide(z);
            n = -n;
        }

        // Algorithm improvements by Bernd Kellner
        int b2pow = 0;

        while ((n & 1) == 0)
        {
            b2pow++;
            n >>>= 1;
        }

        Apcomplex r = z;

        while ((n >>>= 1) > 0)
        {
            z = z.multiply(z);
            if ((n & 1) != 0)
            {
                r = r.multiply(z);
            }
        }

        while (b2pow-- > 0)
        {
            r = r.multiply(r);
        }

        return r;
    }

    /**
     * Square root.
     *
     * @param z The argument.
     *
     * @return Square root of <code>z</code>.
     */

    public static Apcomplex sqrt(Apcomplex z)
        throws ApfloatRuntimeException
    {
        return root(z, 2);
    }

    /**
     * Cube root.
     *
     * @param z The argument.
     *
     * @return Cube root of <code>z</code>.
     */

    public static Apcomplex cbrt(Apcomplex z)
        throws ApfloatRuntimeException
    {
        return root(z, 3);
    }

    /**
     * Positive integer root. The branch that has the smallest angle
     * and same sign of imaginary part as <code>z</code> is always chosen.
     *
     * @param z The argument.
     * @param n Which root to take.
     *
     * @return <code>n</code>:th root of <code>z</code>, that is <code>z<sup>1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>n</code> is zero.
     */

    public static Apcomplex root(Apcomplex z, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (z.real().signum() == 0 && z.imag().signum() == 0)
            {
                throw new ArithmeticException("Zeroth root of zero");
            }

            return new Apcomplex(new Apfloat(1, Apfloat.INFINITE, z.radix()),
                                 Apfloat.ZERO);
        }
        else if (z.real().signum() == 0 && z.imag().signum() == 0)
        {
            return Apcomplex.ZERO;                // Avoid division by zero
        }
        else if (n == 1)
        {
            return z;
        }
        else if (n == 0x8000000000000000L)
        {
            return sqrt(root(z, n / -2));
        }
        else if (n < 0)
        {
            return inverseRoot(z, -n);
        }
        else if (n == 2)
        {
            return z.multiply(inverseRoot(z, 2));
        }
        else    // The case n=3 can't be optimized now because wrong branch would be chosen
        {
            return Apcomplex.ONE.divide(inverseRoot(z, n));
        }
    }

    /**
     * Inverse positive integer root. The branch that has the smallest angle
     * and different sign of imaginary part than <code>z</code> is always chosen.
     *
     * @param z The argument.
     * @param n Which inverse root to take.
     *
     * @return Inverse <code>n</code>:th root of <code>z</code>, that is <code>z<sup>-1/n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If <code>z</code> or <code>n</code> is zero.
     */

    public static Apcomplex inverseRoot(Apcomplex z, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (z.real().signum() == 0 && z.imag().signum() == 0)
        {
            throw new ArithmeticException("Inverse root of zero");
        }
        else if (n == 0)
        {
            return new Apcomplex(new Apfloat(1, Apfloat.INFINITE, z.radix()),
                                 Apfloat.ZERO);
        }
        else if (z.equals(Apcomplex.ONE))
        {
            // Trivial case
            return z;
        }
        else if (n == 0x8000000000000000L)
        {
            return inverseRoot(inverseRoot(z, n / -2), 2);
        }
        else if (n < 0)
        {
            return Apcomplex.ONE.divide(inverseRoot(z, -n));
        }

        long targetPrecision = z.precision();

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate inverse root to infinite precision");
        }

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                divisor = new Apfloat(n, Apfloat.INFINITE, z.radix());

        double doubleReal,
               doubleImag,
               magnitude,
               angle;

        long realScale = z.real().scale(),
             imagScale = z.imag().scale(),
             scaleDiff = Math.max(realScale, imagScale) - Math.min(realScale, imagScale),
             doublePrecision = ApfloatHelper.getDoublePrecision(z.radix()),
             precision = doublePrecision;       // Accuracy of initial guess

        Apcomplex result;

        // Calculate initial guess from z
        if (z.imag().signum() == 0 ||
            (scaleDiff > doublePrecision / 2 || scaleDiff < 0) && realScale > imagScale)        // Detect overflow
        {
            // z.real() is a lot bigger in magnitude than z.imag()
            long scaleQuot = realScale / n,
                 scaleRem = realScale - scaleQuot * n;

            Apfloat tmpReal = z.real().precision(doublePrecision),
                    tmpImag = z.imag().precision(doublePrecision);
            Apcomplex tweak = new Apcomplex(Apcomplex.ZERO,
                                            tmpImag.divide(divisor.multiply(tmpReal)));

            tmpReal = ApfloatMath.scale(tmpReal, scaleRem - tmpReal.scale());   // Allow exponents in exess of doubles'

            if ((magnitude = tmpReal.doubleValue()) >= 0.0)
            {
                doubleReal = Math.pow(magnitude, -1.0 / (double) n);
                doubleImag = 0.0;
            }
            else
            {
                magnitude = Math.pow(-magnitude, -1.0 / (double) n);
                angle = (tmpImag.signum() >= 0 ? -Math.PI : Math.PI) / (double) n;
                doubleReal = magnitude * Math.cos(angle);
                doubleImag = magnitude * Math.sin(angle);
            }

            tmpReal = ApfloatMath.scale(new Apfloat(doubleReal, doublePrecision, z.radix()), -scaleQuot);
            tmpImag = ApfloatMath.scale(new Apfloat(doubleImag, doublePrecision, z.radix()), -scaleQuot);
            result = new Apcomplex(tmpReal, tmpImag);
            result = result.subtract(result.multiply(tweak));               // Must not be real
        }
        else if (z.real().signum() == 0 ||
                 (scaleDiff > doublePrecision / 2 || scaleDiff < 0) && imagScale > realScale)        // Detect overflow
        {
            // z.imag() is a lot bigger in magnitude than z.real()
            long scaleQuot = imagScale / n,
                 scaleRem = imagScale - scaleQuot * n;

            Apfloat tmpReal = z.real().precision(doublePrecision),
                    tmpImag = z.imag().precision(doublePrecision);
            Apcomplex tweak = new Apcomplex(Apfloat.ZERO,
                                            tmpReal.divide(divisor.multiply(tmpImag)));

            tmpImag = ApfloatMath.scale(tmpImag, scaleRem - tmpImag.scale());   // Allow exponents in exess of doubles'

            if ((magnitude = tmpImag.doubleValue()) >= 0.0)
            {
                magnitude = Math.pow(magnitude, -1.0 / (double) n);
                angle = -Math.PI / (2.0 * (double) n);
            }
            else
            {
                magnitude = Math.pow(-magnitude, -1.0 / (double) n);
                angle = Math.PI / (2.0 * (double) n);
            }

            doubleReal = magnitude * Math.cos(angle);
            doubleImag = magnitude * Math.sin(angle);

            tmpReal = ApfloatMath.scale(new Apfloat(doubleReal, doublePrecision, z.radix()), -scaleQuot);
            tmpImag = ApfloatMath.scale(new Apfloat(doubleImag, doublePrecision, z.radix()), -scaleQuot);
            result = new Apcomplex(tmpReal, tmpImag);
            result = result.add(result.multiply(tweak));               // Must not be pure imaginary
        }
        else
        {
            // z.imag() and z.real() approximately the same in magnitude
            long scaleQuot = realScale / n,
                 scaleRemReal = realScale - scaleQuot * n,
                 scaleRemImag = imagScale - scaleQuot * n;

            Apfloat tmpReal = z.real().precision(doublePrecision),
                    tmpImag = z.imag().precision(doublePrecision);

            tmpReal = ApfloatMath.scale(tmpReal, scaleRemReal - tmpReal.scale());       // Allow exponents in exess of doubles'
            tmpImag = ApfloatMath.scale(tmpImag, scaleRemImag - tmpImag.scale());       // Allow exponents in exess of doubles'

            doubleReal = tmpReal.doubleValue();
            doubleImag = tmpImag.doubleValue();

            magnitude = Math.pow(doubleReal * doubleReal + doubleImag * doubleImag, -1.0 / (2.0 * (double) n));
            angle = -Math.atan2(doubleImag, doubleReal) / (double) n;

            doubleReal = magnitude * Math.cos(angle);
            doubleImag = magnitude * Math.sin(angle);

            tmpReal = ApfloatMath.scale(new Apfloat(doubleReal, doublePrecision, z.radix()), -scaleQuot);
            tmpImag = ApfloatMath.scale(new Apfloat(doubleImag, doublePrecision, z.radix()), -scaleQuot);
            result = new Apcomplex(tmpReal, tmpImag);
        }

        int iterations = 0;

        // Compute total number of iterations
        for (long maxPrec = precision; maxPrec < targetPrecision; maxPrec <<= 1)
        {
            iterations++;
        }

        int precisingIteration = iterations;

        // Check where the precising iteration should be done
        for (long minPrec = precision; precisingIteration > 0; precisingIteration--, minPrec <<= 1)
        {
            if ((minPrec - Apcomplex.EXTRA_PRECISION) << precisingIteration >= targetPrecision)
            {
                break;
            }
        }

        // Newton's iteration
        while (iterations-- > 0)
        {
            precision *= 2;
            result = ApfloatHelper.setPrecision(result, Math.min(precision, targetPrecision));

            Apcomplex t = one.subtract(z.multiply(pow(result, n)));
            if (iterations < precisingIteration)
            {
                t = new Apcomplex(t.real().precision(precision / 2),
                                  t.imag().precision(precision / 2));
            }

            result = result.add(result.multiply(t).divide(divisor));

            // Precising iteration
            if (iterations == precisingIteration)
            {
                result = result.add(result.multiply(one.subtract(z.multiply(pow(result, n)))).divide(divisor));
            }
        }

        return ApfloatHelper.setPrecision(result, targetPrecision);
    }

    /**
     * Arithmetic-geometric mean.
     *
     * @param a First argument.
     * @param b Second argument.
     *
     * @return Arithmetic-geometric mean of <code>a</code> and <code>b</code>.
     */

    public static Apcomplex agm(Apcomplex a, Apcomplex b)
        throws ApfloatRuntimeException
    {
        if (a.real().signum() == 0 && a.imag().signum() == 0 ||
            b.real().signum() == 0 && b.imag().signum() == 0)         // Would not converge quadratically
        {
            return Apcomplex.ZERO;
        }

        long workingPrecision = Math.min(a.precision(), b.precision()),
             targetPrecision = Math.max(a.precision(), b.precision());

        if (workingPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate agm to infinite precision");
        }

        // Some minimum precision is required for the algorithm to work
        workingPrecision = ApfloatHelper.extendPrecision(workingPrecision);
        a = ApfloatHelper.ensurePrecision(a, workingPrecision);
        b = ApfloatHelper.ensurePrecision(b, workingPrecision);

        long precision = 0,
             halfWorkingPrecision = (workingPrecision + 1) / 2;
        final long CONVERGING = 1000;           // Arbitrarily chosen value...
        Apfloat two = new Apfloat(2, Apfloat.INFINITE, a.radix());

        // First check convergence
        while (precision < CONVERGING && precision < halfWorkingPrecision)
        {
            Apcomplex t = a.add(b).divide(two);
            b = sqrt(a.multiply(b));
            a = t;

            // Conserve precision in case of accumulating round-off errors
            a = ApfloatHelper.ensurePrecision(a, workingPrecision);
            b = ApfloatHelper.ensurePrecision(b, workingPrecision);

            precision = a.equalDigits(b);
        }

        // Now we know quadratic convergence
        while (precision <= halfWorkingPrecision)
        {
            Apcomplex t = a.add(b).divide(two);
            b = sqrt(a.multiply(b));
            a = t;

            // Conserve precision in case of accumulating round-off errors
            a = ApfloatHelper.ensurePrecision(a, workingPrecision);
            b = ApfloatHelper.ensurePrecision(b, workingPrecision);

            precision *= 2;
        }

        return ApfloatHelper.setPrecision(a.add(b).divide(two), targetPrecision);
    }

    /**
     * Natural logarithm.<p>
     *
     * The logarithm is calculated using the arithmetic-geometric mean.
     * See the Borweins' book for the formula.
     *
     * @param z The argument.
     *
     * @return Natural logarithm of <code>z</code>.
     *
     * @exception java.lang.ArithmeticException If <code>z</code> is zero.
     */

    public static Apcomplex log(Apcomplex z)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (z.real().signum() >= 0 && z.imag().signum() == 0)
        {
            return ApfloatMath.log(z.real());
        }

        // Calculate the log using 1 / radix <= |z| < 1 and the log addition formula
        // because the agm converges badly for big z

        long targetPrecision = z.precision();

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate logarithm to infinite precision");
        }

        Apfloat imagBias;

        // Scale z so that real part of z is always >= 0, that is its angle is -pi/2 <= angle(z) <= pi/2 to avoid possible instability near z.imag() = +-pi
        if (z.real().signum() < 0)
        {
            Apfloat pi = ApfloatHelper.extendPrecision(ApfloatMath.pi(targetPrecision, z.radix()), z.radix() <= 3 ? 1 : 0);     // pi may have 1 digit more than pi/2

            if (z.imag().signum() >= 0)
            {
                imagBias = pi;
            }
            else
            {
                imagBias = ApfloatMath.negate(pi);
            }

            z = negate(z);
        }
        else
        {
            // No bias
            imagBias = Apfloat.ZERO;
        }

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                x = abs(z);

        long originalScale = z.scale();

        z = scale(z, -originalScale);   // Set z's scale to zero

        Apfloat radixPower;
        if (originalScale == 0)
        {
            radixPower = Apfloat.ZERO;
        }
        else
        {
            Apfloat logRadix = ApfloatHelper.extendPrecision(ApfloatMath.logRadix(targetPrecision, z.radix()));
            radixPower = new Apfloat(originalScale, Apfloat.INFINITE, z.radix()).multiply(logRadix);
        }

        Apcomplex result = ApfloatHelper.extendPrecision(rawLog(z)).add(radixPower);

        // If the absolute value of the argument is close to 1, the real part of the result is less accurate
        // If the angle of the argument is close to zero, the imaginary part of the result is less accurate
        long finalRealPrecision = Math.max(targetPrecision - one.equalDigits(x), 1),
             finalImagPrecision = Math.max(targetPrecision - 1 + result.imag().scale(), 1);     // Scale of pi/2 is always 1

        return new Apcomplex(result.real().precision(finalRealPrecision),
                             result.imag().precision(finalImagPrecision).add(imagBias));
    }

    // Raw logarithm, regardless of z
    // Doesn't work for really big z, but is faster if used alone for small numbers
    private static Apcomplex rawLog(Apcomplex z)
        throws ApfloatRuntimeException
    {
        assert (z.real().signum() != 0 || z.imag().signum() != 0);      // Infinity

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix());

        final int EXTRA_PRECISION = 25;

        long targetPrecision = z.precision(),
             workingPrecision = ApfloatHelper.extendPrecision(targetPrecision),
             n = targetPrecision / 2 + EXTRA_PRECISION;                 // Very rough estimate

        z = ApfloatHelper.extendPrecision(z, EXTRA_PRECISION);

        Apfloat e = one.precision(workingPrecision);
        e = ApfloatMath.scale(e, -n);
        z = scale(z, -n);

        Apfloat agme = ApfloatHelper.extendPrecision(ApfloatMath.agm(one, e));
        Apcomplex agmez = ApfloatHelper.extendPrecision(agm(one, z));

        Apfloat pi = ApfloatHelper.extendPrecision(ApfloatMath.pi(targetPrecision, z.radix()));
        Apcomplex log = pi.multiply(agmez.subtract(agme)).divide(new Apfloat(2, Apfloat.INFINITE, z.radix()).multiply(agme).multiply(agmez));

        return ApfloatHelper.setPrecision(log, targetPrecision);
    }

    /**
     * Exponent function.
     * Calculated using Newton's iteration for the inverse of logarithm.
     *
     * @param z The argument.
     *
     * @return <code>e<sup>z</sup></code>.
     */

    public static Apcomplex exp(Apcomplex z)
        throws ApfloatRuntimeException
    {
        if (z.imag().signum() == 0)
        {
            return ApfloatMath.exp(z.real());
        }

        int radix = z.radix();
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, radix);

        long doublePrecision = ApfloatHelper.getDoublePrecision(radix);

        // If the real part of the argument is close to 0, the result is more accurate
        // The imaginary part must be scaled to the range of -pi ... pi, which may limit the precision
        long targetPrecision = (z.imag().precision() >= z.imag().scale() ?
                                Math.min(Util.ifFinite(z.real().precision(), z.real().precision() + Math.max(1 - z.real().scale(), 0)),
                                         Util.ifFinite(z.imag().precision(), 1 + z.imag().precision() - z.imag().scale())) :
                                0);

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate exponent to infinite precision");
        }
        else if (z.real().compareTo(new Apfloat((double) Long.MAX_VALUE * Math.log((double) radix), doublePrecision, radix)) >= 0)
        {
            throw new ApfloatRuntimeException("Overflow");
        }
        else if (targetPrecision == 0)
        {
            throw new ApfloatRuntimeException("Complete loss of accurate digits in imaginary part");
            // return ApfloatMath.exp(z.real().precision(1));
        }

        Apfloat pi = ApfloatMath.pi(targetPrecision, radix),    // This is precalculated for initial check only
                twoPi = pi.add(pi),
                halfPi = pi.divide(new Apfloat(2, Apfloat.INFINITE, radix)),
                resultReal;
        Apcomplex resultImag;
        boolean negateResult = false;                           // If the final result is to be negated

        // Scale z so that -pi < z.imag() <= pi
        Apfloat zImag = ApfloatMath.fmod(z.imag(), twoPi);
        if (zImag.compareTo(pi) > 0)
        {
            zImag = zImag.subtract(twoPi);
        }
        else if (zImag.compareTo(ApfloatMath.negate(pi)) <= 0)
        {
            zImag = zImag.add(twoPi);
        }
        // More, scale z so that -pi/2 < z.imag() <= pi/2 to avoid instability near z.imag() = +-pi
        if (zImag.compareTo(halfPi) > 0)
        {
            // exp(z - i*pi) = exp(z)/exp(i*pi) = -exp(z)
            zImag = zImag.subtract(pi);
            negateResult = true;
        }
        else if (zImag.compareTo(ApfloatMath.negate(halfPi)) <= 0)
        {
            // exp(z + i*pi) = exp(z)*exp(i*pi) = -exp(z)
            zImag = zImag.add(pi);
            negateResult = true;
        }
        z = new Apcomplex(z.real(), zImag);

        // First handle the real part

        if (z.real().signum() == 0)
        {
            resultReal = one;
        }
        else if (z.real().scale() < -doublePrecision / 2)
        {
            // Taylor series: exp(x) = 1 + x + x^2/2 + ...

            resultReal = one.precision(-2 * z.real().scale()).add(z.real());
        }
        else
        {
            // Approximate starting value for iteration

            // An overflow should not occur
            double doubleValue = z.real().doubleValue() / Math.log((double) radix),
                   integerPart = Math.floor(doubleValue),
                   fractionalPart = doubleValue - integerPart;

            resultReal = new Apfloat(Math.pow((double) radix, fractionalPart), doublePrecision, radix);
            resultReal = ApfloatMath.scale(resultReal, (long) integerPart);

            // Initial precision is reduced if z.real() is very big
            int integerPartDigits = (integerPart > 0 ? (int) Math.floor(Math.log(integerPart + 0.5) / Math.log((double) radix)) : 0);
            resultReal = resultReal.precision(Math.max(1, doublePrecision - integerPartDigits));
        }

        // Then handle the imaginary part

        if (z.imag().scale() < -doublePrecision / 2)
        {
            // Taylor series: exp(z) = 1 + z + z^2/2 + ...

            resultImag = new Apcomplex(one, z.imag());
        }
        else
        {
            // Approximate starting value for iteration

            double doubleImag = z.imag().doubleValue();
            resultImag = new Apcomplex(new Apfloat(Math.cos(doubleImag), doublePrecision, radix),
                                       new Apfloat(Math.sin(doubleImag), doublePrecision, radix));
        }

        // Starting value is (real part starting value) * (imag part starting value)
        Apcomplex result = resultReal.multiply(resultImag);

        long precision = result.precision();    // Accuracy of initial guess

        // Precalculate the needed values once to the required precision
        ApfloatMath.logRadix(targetPrecision, radix);

        int iterations = 0;

        // Compute total number of iterations
        for (long maxPrec = precision; maxPrec < targetPrecision; maxPrec <<= 1)
        {
            iterations++;
        }

        int precisingIteration = iterations;

        // Check where the precising iteration should be done
        for (long minPrec = precision; precisingIteration > 0; precisingIteration--, minPrec <<= 1)
        {
            if ((minPrec - Apcomplex.EXTRA_PRECISION) << precisingIteration >= targetPrecision)
            {
                break;
            }
        }

        z = ApfloatHelper.extendPrecision(z);

        // Newton's iteration
        while (iterations-- > 0)
        {
            precision *= 2;
            result = ApfloatHelper.setPrecision(result, Math.min(precision, targetPrecision));

            Apcomplex t = log(result);
            t = lastIterationExtendPrecision(iterations, precisingIteration, t);
            t = z.subtract(t);

            if (iterations < precisingIteration)
            {
                t = new Apcomplex(t.real().precision(precision / 2),
                                  t.imag().precision(precision / 2));
            }

            result = lastIterationExtendPrecision(iterations, precisingIteration, result);
            result = result.add(result.multiply(t));

            // Precising iteration
            if (iterations == precisingIteration)
            {
                t = log(result);
                t = lastIterationExtendPrecision(iterations, -1, t);

                result = lastIterationExtendPrecision(iterations, -1, result);
                result = result.add(result.multiply(z.subtract(t)));
            }
        }

        return ApfloatHelper.setPrecision(negateResult ? negate(result) : result, targetPrecision);
    }

    // Extend the precision on last iteration
    private static Apcomplex lastIterationExtendPrecision(int iterations, int precisingIteration, Apcomplex z)
    {
        return (iterations == 0 && precisingIteration != 0 ? ApfloatHelper.extendPrecision(z) : z);
    }

    /**
     * Arbitrary power. Calculated using <code>log()</code> and <code>exp()</code>.<p>
     *
     * @param z The base.
     * @param w The exponent.
     *
     * @return <code>z<sup>w</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>z</code> and <code>w</code> are zero.
     */

    public static Apcomplex pow(Apcomplex z, Apcomplex w)
    {
        if (w.real().signum() == 0 && w.imag().signum() == 0)
        {
            if (z.real().signum() == 0 && z.imag().signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apcomplex(new Apfloat(1, Apfloat.INFINITE, z.radix()));
        }
        else if (z.real().signum() == 0 && z.imag().signum() == 0 || z.equals(Apcomplex.ONE) || w.equals(Apcomplex.ONE))
        {
            return z;
        }

        long targetPrecision = Math.min(z.precision(), w.precision());

        if (targetPrecision == Apfloat.INFINITE)
        {
            throw new ApfloatRuntimeException("Cannot calculate power to infinite precision");
        }

        return exp(w.multiply(log(z)));
    }

    /**
     * Inverse cosine. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse cosine of <code>z</code>.
     */

    public static Apcomplex acos(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one),
                  w = i.multiply(log(z.add(sqrt(z.multiply(z).subtract(one)))));

        if (z.real().signum() * z.imag().signum() >= 0)
        {
            return negate(w);
        }
        else
        {
            return w;
        }
    }

    /**
     * Inverse hyperbolic cosine. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse hyperbolic cosine of <code>z</code>.
     */

    public static Apcomplex acosh(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix());

        if (z.real().signum() >= 0)
        {
            return log(z.add(sqrt(z.multiply(z).subtract(one))));
        }
        else
        {
            return log(z.subtract(sqrt(z.multiply(z).subtract(one))));
        }
    }

    /**
     * Inverse sine. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse sine of <code>z</code>.
     */

    public static Apcomplex asin(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one);

        if (z.imag().signum() >= 0)
        {
            return i.multiply(log(sqrt(one.subtract(z.multiply(z))).subtract(i.multiply(z))));
        }
        else
        {
            return negate(i.multiply(log(i.multiply(z).add(sqrt(one.subtract(z.multiply(z)))))));
        }
    }

    /**
     * Inverse hyperbolic sine. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse hyperbolic sine of <code>z</code>.
     */

    public static Apcomplex asinh(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix());

        if (z.real().signum() >= 0)
        {
            return log(sqrt(z.multiply(z).add(one)).add(z));
        }
        else
        {
            return negate(log(sqrt(z.multiply(z).add(one)).subtract(z)));
        }
    }

    /**
     * Inverse tangent. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse tangent of <code>z</code>.
     *
     * @exception java.lang.ArithmeticException If <code>z == <i>i</i></code>.
     */

    public static Apcomplex atan(Apcomplex z)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one);

        return log(i.add(z).divide(i.subtract(z))).multiply(i).divide(two);
    }

    /**
     * Inverse hyperbolic tangent. Calculated using <code>log()</code>.
     *
     * @param z The argument.
     *
     * @return Inverse hyperbolic tangent of <code>z</code>.
     *
     * @exception java.lang.ArithmeticException If <code>z</code> is 1 or -1.
     */

    public static Apcomplex atanh(Apcomplex z)
        throws ArithmeticException, ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());

        return log(one.add(z).divide(one.subtract(z))).divide(two);
    }

    /**
     * Cosine. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Cosine of <code>z</code>.
     */

    public static Apcomplex cos(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one),
                  w = exp(i.multiply(z));

        return (w.add(one.divide(w))).divide(two);
    }

    /**
     * Hyperbolic cosine. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Hyperbolic cosine of <code>z</code>.
     */

    public static Apcomplex cosh(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex w = exp(z);

        return (w.add(one.divide(w))).divide(two);
    }

    /**
     * Sine. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Sine of <code>z</code>.
     */

    public static Apcomplex sin(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one),
                  w = exp(i.multiply(z));

        return one.divide(w).subtract(w).multiply(i).divide(two);
    }

    /**
     * Hyperbolic sine. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Hyperbolic sine of <code>z</code>.
     */

    public static Apcomplex sinh(Apcomplex z)
        throws ApfloatRuntimeException
    {
        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex w = exp(z);

        return (w.subtract(one.divide(w))).divide(two);
    }

    /**
     * Tangent. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Tangent of <code>z</code>.
     *
     * @exception java.lang.ArithmeticException If <code>z</code> is &pi;/2 + n &pi; where n is an integer.
     */

    public static Apcomplex tan(Apcomplex z)
        throws ApfloatRuntimeException
    {
        boolean negate = z.imag().signum() > 0;
        z = (negate ? negate(z) : z);

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex i = new Apcomplex(Apfloat.ZERO, one),
                  w = exp(two.multiply(i).multiply(z));

        w = i.multiply(one.subtract(w)).divide(one.add(w));

        return (negate ? negate(w) : w);
    }

    /**
     * Hyperbolic tangent. Calculated using <code>exp()</code>.
     *
     * @param z The argument.
     *
     * @return Hyperbolic tangent of <code>z</code>.
     *
     * @exception java.lang.ArithmeticException If <code>z</code> is <i>i</i> (&pi;/2 + n &pi;) where n is an integer.
     */

    public static Apcomplex tanh(Apcomplex z)
        throws ApfloatRuntimeException
    {
        boolean negate = z.real().signum() < 0;
        z = (negate ? negate(z) : z);

        Apfloat one = new Apfloat(1, Apfloat.INFINITE, z.radix()),
                two = new Apfloat(2, Apfloat.INFINITE, z.radix());
        Apcomplex w = exp(two.multiply(z));

        w = w.subtract(one).divide(w.add(one));

        return (negate ? negate(w) : w);
    }
}
