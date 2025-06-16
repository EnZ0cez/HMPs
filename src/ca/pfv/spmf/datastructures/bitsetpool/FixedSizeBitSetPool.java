package ca.pfv.spmf.datastructures.bitsetpool;

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
 * This class is an implementation of a pool of bitsets having a fixed size. The
 * idea is that a program using the pool can obtain a bitset and release it so
 * that it can be reused later for other purposes without allocating new memory.
 * It is assumed that the user will not release the same bitset twice.
 * 
 * @see BitSet
 * @author Philippe Fournier-Viger
 */
public class FixedSizeBitSetPool extends BitSetPool {

	/** Size of each bitset */
	private final int sizeOfEachBitset;


	/**
	 * Constructor
	 * 
	 * @param numberOfBitsets  the number of bitsets
	 * @param sizeOfEachBitset the size of each bitset
	 */
	public FixedSizeBitSetPool(int initialPoolSize, int sizeOfEachBitset) {
		super(initialPoolSize);
		
		
		// Save the parameters
		this.sizeOfEachBitset = sizeOfEachBitset;
	}

	/** Instantiate a new BitSet */
	protected BitSet instantiateNewBitSet() {
		return new BitSet(sizeOfEachBitset);
	}

}