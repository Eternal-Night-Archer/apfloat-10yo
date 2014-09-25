package org.apfloat;

/**
 * Various mathematical functions for arbitrary precision integers.
 *
 * @version 1.0.2
 * @author Mikko Tommila
 */

public class ApintMath
{
    private ApintMath()
    {
    }

    /**
     * Integer power.
     *
     * @param x Base of the power operator.
     * @param n Exponent of the power operator.
     *
     * @return <code>x</code> to the <code>n</code>:th power, that is <code>x<sup>n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>x</code> and <code>n</code> are zero.
     */

    public static Apint pow(Apint x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apint(1, x.radix());
        }
        else if (n < 0)
        {
            return Apint.ZERO;
        }

        // Algorithm improvements by Bernd Kellner
        int b2pow = 0;

        while ((n & 1) == 0)
        {
            b2pow++;
            n >>= 1;
        }

        Apint r = x;

        while ((n >>= 1) > 0)
        {
            x = x.multiply(x);
            if ((n & 1) != 0)
            {
                r = r.multiply(x);
            }
        }

        while (b2pow-- > 0)
        {
            r = r.multiply(r);
        }

        return r;
    }

    /**
     * Square root and remainder.
     *
     * @param x The argument.
     *
     * @return An array of two apints: <code>[q, r]</code>, where <code>q<sup>2</sup> + r = x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>x</code> is negative.
     */

    public static Apint[] sqrt(Apint x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        return root(x, 2);
    }

    /**
     * Cube root and remainder.
     *
     * @param x The argument.
     *
     * @return An array of two apints: <code>[q, r]</code>, where <code>q<sup>3</sup> + r = x</code>.
     */

    public static Apint[] cbrt(Apint x)
        throws ApfloatRuntimeException
    {
        return root(x, 3);
    }

    /**
     * Positive integer root and remainder.<p>
     *
     * Returns the <code>n</code>:th root of <code>x</code>,
     * that is <code>x<sup>1/n</sup></code>, rounded towards zero.
     *
     * @param x The argument.
     * @param n Which root to take.
     *
     * @return An array of two apints: <code>[q, r]</code>, where <code>q<sup>n</sup> + r = x</code>.
     *
     * @exception java.lang.ArithmeticException If <code>n</code> and <code>x</code> are zero, or <code>x</code> is negative and <code>n</code> is even.
     */

    public static Apint[] root(Apint x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zeroth root of zero");
            }

            Apint one = new Apint(1, x.radix());
            return new Apint[] { one, x.subtract(one) };
        }
        else if (x.signum() == 0)
        {
            return new Apint[] { x, x };                        // Avoid division by zero
        }
        else if (x.equals(Apint.ONE) || n == 1)
        {
            return new Apint[] { x, Apint.ZERO };
        }
        else if (n < 0)
        {
            return new Apint[] { Apint.ZERO, x };               // 1 / x where x > 1
        }

        long precision = x.scale() / n + Apint.EXTRA_PRECISION;
        Apfloat approxX = x.precision(precision);
        Apfloat approxRoot;

        if (n == 2)
        {
            approxRoot = approxX.multiply(ApfloatMath.inverseRoot(approxX, 2));
        }
        else if (n == 3)
        {
            approxRoot = approxX.multiply(ApfloatMath.inverseRoot(approxX.multiply(approxX), 3));
        }
        else
        {
            approxRoot = ApfloatMath.inverseRoot(ApfloatMath.inverseRoot(approxX, n), 1);
        }

        Apint root = approxRoot.truncate(),                             // May be one too big or one too small
              pow = pow(root, n);

        if (abs(pow).compareTo(abs(x)) > 0)
        {
            // Approximate root was one too big

            pow = (x.signum() >= 0 ? powXMinus1(pow, root, n) : powXPlus1(pow, root, n));
            root = root.subtract(new Apint(x.signum(), x.radix()));
        }
        else
        {
            // Approximate root was correct or one too small

            Apint powPlus1 = (x.signum() >= 0 ? powXPlus1(pow, root, n) : powXMinus1(pow, root, n));

            if (abs(powPlus1).compareTo(abs(x)) <= 0)
            {
                // Approximate root was one too small

                pow = powPlus1;
                root = root.add(new Apint(x.signum(), x.radix()));
            }
        }

        Apint remainder = x.subtract(pow);

        assert (remainder.signum() * x.signum() >= 0);

        return new Apint[] { root, remainder };
    }

    private static Apint powXMinus1(Apint pow, Apint x, long n)
    {
        Apint one = new Apint(1, x.radix());

        if (n == 2)
        {
            // (x - 1)^2 = x^2 - 2*x + 1
            pow = pow.subtract(x).subtract(x).add(one);
        }
        else if (n == 2)
        {
            // (x - 1)^3 = x^3 - 3*x^2 + 3*x - 1 = x^3 - 3*x*(x - 1) - 1
            pow = pow.subtract(new Apint(3, x.radix()).multiply(x).multiply(x.subtract(one))).subtract(one);
        }
        else
        {
            pow = pow(x.subtract(one), n);
        }

        return pow;
    }

    private static Apint powXPlus1(Apint pow, Apint x, long n)
    {
        Apint one = new Apint(1, x.radix());

        if (n == 2)
        {
            // (x + 1)^2 = x^2 + 2*x + 1
            pow = pow.add(x).add(x).add(one);
        }
        else if (n == 2)
        {
            // (x + 1)^3 = x^3 + 3*x^2 + 3*x + 1 = x^3 + 3*x*(x + 1) + 1
            pow = pow.add(new Apint(3, x.radix()).multiply(x).multiply(x.add(one))).add(one);
        }
        else
        {
            pow = pow(x.add(one), n);
        }

        return pow;
    }

    /**
     * Returns an apint whose value is <code>-x</code>.
     *
     * @param x The argument.
     *
     * @return <code>-x</code>.
     */

    public static Apint negate(Apint x)
        throws ApfloatRuntimeException
    {
        return Apint.ZERO.subtract(x);
    }

    /**
     * Absolute value.
     *
     * @param x The argument.
     *
     * @return Absolute value of <code>x</code>.
     */

    public static Apint abs(Apint x)
        throws ApfloatRuntimeException
    {
        if (x.signum() >= 0)
        {
            return x;
        }
        else
        {
            return negate(x);
        }
    }

    /**
     * Copy sign from one argument to another.
     *
     * @param x The value whose sign is to be adjusted.
     * @param x The value whose sign is to be used.
     *
     * @return <code>x</code> with its sign changed to match the sign of <code>y</code>.
     */

    private static Apint copySign(Apint x, Apint y)
        throws ApfloatRuntimeException
    {
        if (y.signum() == 0)
        {
            return y;
        }
        else if (x.signum() != y.signum())
        {
            return negate(x);
        }
        else
        {
            return x;
        }
    }

    /**
     * Multiply by a power of the radix.
     * Any rounding will occur towards zero.
     *
     * @param x The argument.
     * @param scale The scaling factor.
     *
     * @return <code>x * x.radix()<sup>scale</sup></code>.
     */

    public static Apint scale(Apint x, long scale)
        throws ApfloatRuntimeException
    {
        return ApfloatMath.scale(x, scale).truncate();
    }

    /**
     * Quotient and remainder.
     *
     * @param x The dividend.
     * @param y The divisor.
     *
     * @return An array of two apints: <code>[quotient, remainder]</code>, that is <code>[x / y, x % y]</code>.
     *
     * @exception java.lang.ArithmeticException In case the divisor is zero.
     */

    public static Apint[] div(Apint x, Apint y)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (y.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        else if (x.signum() == 0)
        {
            // 0 / x = 0
            return new Apint[] { x, x };
        }
        else if (y.equals(Apint.ONE))
        {
            // x / 1 = x
            return new Apint[] { x, Apint.ZERO };
        }

        long precision;
        Apfloat tx, ty;
        Apint a, b, q, r;

        a = abs(x);
        b = abs(y);

        if (a.compareTo(b) < 0)
        {
            return new Apint[] { Apint.ZERO, x };  // abs(x) < abs(y)
        }
        else
        {
            precision = x.scale() - y.scale() + Apint.EXTRA_PRECISION;        // Some extra precision to avoid round-off errors
        }

        tx = x.precision(precision);
        ty = y.precision(precision);

        q = tx.divide(ty).truncate();           // Approximate division

        a = a.subtract(abs(q.multiply(y)));

        if (a.compareTo(b) >= 0)                // Fix division round-off error
        {
            q = q.add(new Apint(x.signum() * y.signum(), x.radix()));
            a = a.subtract(b);
        }
        else if (a.signum() < 0)                // Fix division round-off error
        {
            q = q.subtract(new Apint(x.signum() * y.signum(), x.radix()));
            a = a.add(b);
        }

        r = copySign(a, x);

        return new Apint[] { q, r };
    }

    /**
     * Greatest common divisor.
     * This method returns a positive number even if one of <code>a</code>
     * and <code>b</code> is negative.
     *
     * @param a First argument.
     * @param b Second argument.
     *
     * @return Greatest common divisor of <code>a</code> and <code>b</code>.
     */

    public static Apint gcd(Apint a, Apint b)
        throws ApfloatRuntimeException
    {
        while (b.signum() != 0)
        {
            Apint r = a.mod(b);
            a = b;
            b = r;
        }

        return abs(a);
    }

    /**
     * Least common multiple.
     * This method returns a positive number even if one of <code>a</code>
     * and <code>b</code> is negative.
     *
     * @param a First argument.
     * @param b Second argument.
     *
     * @return Least common multiple of <code>a</code> and <code>b</code>.
     */

    public static Apint lcm(Apint a, Apint b)
        throws ApfloatRuntimeException
    {
        if (a.signum() == 0 && b.signum() == 0)
        {
            return Apint.ZERO;
        }
        else
        {
            return abs(a.multiply(b)).divide(gcd(a, b));
        }
    }

    /**
     * Modular multiplication. Returns <code>a * b % m</code>
     *
     * @param a First argument.
     * @param b Second argument.
     * @param m Modulus.
     *
     * @return <code>a * b mod m</code>
     */

    public static Apint modMultiply(Apint a, Apint b, Apint m)
        throws ApfloatRuntimeException
    {
        return a.multiply(b).mod(m);
    }

    private static Apint modMultiply(Apint x1, Apint x2, Apint y, Apfloat inverseY)
        throws ApfloatRuntimeException
    {
        Apint x = x1.multiply(x2);

        if (x.signum() == 0)
        {
            // 0 % x = 0
            return x;
        }

        long precision = x.scale() - y.scale() + Apfloat.EXTRA_PRECISION;       // Some extra precision to avoid round-off errors
        Apint a, b, t;

        a = abs(x);
        b = abs(y);

        if (a.compareTo(b) < 0)
        {
            return x;                           // abs(x) < abs(y)
        }

        t = x.multiply(inverseY.precision(precision)).truncate();               // Approximate division

        a = a.subtract(abs(t.multiply(y)));

        if (a.compareTo(b) >= 0)                // Fix division round-off error
        {
            a = a.subtract(b);
        }
        else if (a.signum() < 0)                // Fix division round-off error
        {
            a = a.add(b);
        }

        t = copySign(a, x);

        return t;
    }

    /**
     * Modular power.
     *
     * @param a Base.
     * @param b Exponent. Should be non-negative, since the modulus can't be factorized with this implementation.
     * @param m Modulus.
     *
     * @return <code>a<sup>b</sup> mod m</code>
     */

    public static Apint modPow(Apint a, Apint b, Apint m)
        throws ApfloatRuntimeException
    {
        if (b.signum() == 0)
        {
            if (a.signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apint(1, a.radix());
        }
        else if (m.signum() == 0)
        {
            return m;                           // By definition
        }
        else if (b.signum() < 0)
        {
            throw new ApfloatRuntimeException("Negative exponent not supported; unable to factorize modulus");
        }

        m = abs(m);

        Apfloat inverseModulus = ApfloatMath.inverseRoot(m, 1, m.scale() + Apfloat.EXTRA_PRECISION);
        a = a.mod(m);

        Apint two = new Apint(2, b.radix());    // Sub-optimal; the divisor could be some power of two
        Apint[] qr;

        while ((qr = div(b, two))[1].signum() == 0)
        {
            a = modMultiply(a, a, m, inverseModulus);
            b = qr[0];
        }

        Apint r = a;
        qr = div(b, two);

        while ((b = qr[0]).signum() > 0)
        {
            a = modMultiply(a, a, m, inverseModulus);
            qr = div(b, two);
            if (qr[1].signum() != 0)
            {
                r = modMultiply(r, a, m, inverseModulus);
            }
        }

        return r;
    }
}
