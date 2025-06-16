package ca.pfv.spmf.datastructures.bitsetpool;

import java.util.Arrays;
import java.util.BitSet;

/* This file is copyright (c) 2023 Philippe Fournier-Viger
* 
* This file is part of the SPMF DATA MINING SOFTWARE
* (http://www.philippe-fournier-viger.com/spmf).
* 
* SPMF is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* SPMF. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * This class is an implementation of a pool of bitsets where the size of each bitset is not fixed. The
 * idea is that a program using the pool can obtain a bitset and release it so
 * that it can be reused later for other purposes without allocating new memory.
 * It is assumed that the user will not release the same bitset twice.
 * 
 * @see BitSet
 * @author Philippe Fournier-Viger
 */
public class BitSetPool {

	/** The number of bitsets that are in the pool */
	private int actualPoolSize;

	/** Initial pool size */
	private final int initialPoolSize;

	/** The pool of bitsets */
	private BitSet[] pool;

	/**
	 * Constructor
	 * 
	 * @param numberOfBitsets  the number of bitsets
	 * @param sizeOfEachBitset the size of each bitset
	 */
	public BitSetPool(int initialPoolSize) {
		this.initialPoolSize = initialPoolSize;

		// initialize the pool
		clear();
	}

	/**
	 * Obtain a bitset that is unused from the pool or create a new one
	 * if none are available.
	 * 
	 * @return a bitset
	 */
	public BitSet getBitSet() {
		// If the pool is empty
		if (actualPoolSize == 0) {
			BitSet bitset = instantiateNewBitSet();
			return bitset;
		} else {
			BitSet bitset = pool[--actualPoolSize];
			// Otherwise
			return bitset;
		}
	}
	
	/** Instantiate a new BitSet */
	protected BitSet instantiateNewBitSet() {
		return new BitSet();
	}

	/**
	 * Put an unused bitset in the pool so that it can be reused.
	 * Important: it is assumed that the bitset is not already in the pool
	 *  (this is not verified by this method).
	 * 
	 * @param bitset the bitset
	 */
	public void releaseBitSet(BitSet bitset) {
		bitset.clear();

		if (actualPoolSize == pool.length) {
			resize();
		}
		pool[actualPoolSize++] = bitset;
	}

	/**
	 * Resize the internal pool structure to double its size so that we can put more elements.
	 */
	private void resize() {
		BitSet[] newPool = new BitSet[2 * pool.length];
		System.arraycopy(pool, 0, newPool, 0, pool.length);
		pool = newPool;
	}

	/** Reset the pool and empty everything so that it can be used as a new pool*/
	public void clear() {
		actualPoolSize = 0;
		pool = new BitSet[initialPoolSize];
	}
	
	/** Method for debugging that print pool information in the console
	 */
	void printPoolInformation() {
		System.out.println(" POOL: actualPoolSize = " + actualPoolSize + " Pool array = " + Arrays.toString(pool));
	}

}