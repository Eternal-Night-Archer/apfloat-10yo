package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.NTTBuilder;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.Util;

/**
 * Creates Number Theoretic Transforms suitable for the
 * specified length and based on available memory, for the
 * <code>double</code> type.
 *
 * @see DoubleTableFNTStrategy
 * @see DoubleSixStepFNTStrategy
 * @see DoubleTwoPassFNTStrategy
 * @see DoubleFactor3NTTStrategy
 * @see DoubleFactor3SixStepNTTStrategy
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class DoubleNTTBuilder
    implements NTTBuilder
{
    /**
     * Default constructor.
     */

    public DoubleNTTBuilder()
    {
    }

    public NTTStrategy createNTT(long size)
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int cacheSize = ctx.getCacheL1Size() / 8;
        long maxMemoryBlockSize = ctx.getMaxMemoryBlockSize() / 8;

        NTTStrategy transform;
        boolean useFactor3 = false;

        size = Util.round23up(size);        // Round up to the nearest power of two or three times a power of two
        long power2size = (size & -size);   // Power-of-two factor of the above
        if (size != power2size)
        {
            // A factor of three will be used, so the power-of-two part is one third of the whole transform length
            useFactor3 = true;
        }

        // Select transform for the power-of-two part
        if (power2size <= cacheSize / 2)
        {
            // The whole transform plus w-table fits into the cache, so use the simplest approach
            transform = new DoubleTableFNTStrategy();
        }
        else if (power2size <= maxMemoryBlockSize && power2size <= Integer.MAX_VALUE)
        {
            // The whole transform fits into the available main memory, so use a six-step in-memory approach
            transform = new DoubleSixStepFNTStrategy();
        }
        else
        {
            // The whole transform won't fit into available memory, so use a two-pass disk based approach
            transform = new DoubleTwoPassFNTStrategy();
        }

        if (useFactor3)
        {
            // Allow using a factor of three in any of the above selected transforms
            if (size <= maxMemoryBlockSize && size <= Integer.MAX_VALUE && transform instanceof DoubleSixStepFNTStrategy)
            {
                // The whole transform (including the factor of 3) fits into the available main memory, so use a special in-memory approach
                transform = new DoubleFactor3SixStepNTTStrategy((DoubleSixStepFNTStrategy) transform);
            }
            else
            {
                // Either basic table FNT or two-pass transform or six-step but with whole transform not fitting in memory
                transform = new DoubleFactor3NTTStrategy(transform);
            }
        }

        return transform;
    }
}
