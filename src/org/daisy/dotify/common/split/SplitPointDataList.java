package org.daisy.dotify.common.split;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides split point data
 * @author Joel Håkansson
 *
 * @param <T> the type of split point units
 */
public final class SplitPointDataList<T extends SplitPointUnit> implements SplitPointDataSource<T> {
	/**
	 * Provides an empty manager.
	 */
	@SuppressWarnings("rawtypes")
    public static final SplitPointDataList EMPTY_MANAGER = new SplitPointDataList<>();
	@SuppressWarnings("rawtypes")
	private static final Supplements EMPTY_SUPPLEMENTS = new Supplements() {
		@Override
		public Object get(String id) {
			return null;
		}
	};
	private final List<T> units;
	private final Supplements<T> supplements;
	private final int offset;

	/**
	 * Creates a new instance with the specified units
	 * @param units the units
	 */
	@SafeVarargs
	public SplitPointDataList(T ... units) {
		this(Arrays.asList(units));
	}
	
	/**
	 * Creates a new instance with the specified units
	 * @param units the units
	 */
	public SplitPointDataList(List<T> units) {
		this(units, null);
	}
	
	/**
	 * Creates a new instance with no units
	 */
	public SplitPointDataList() {
		this(Collections.emptyList(), null);
	}
	
    /**
     * Returns an empty manager
     * @param <T> the type of split point units
     * @return returns an empty manager
     */
    @SuppressWarnings("unchecked")
    public static final <T extends SplitPointUnit> SplitPointDataSource<T> emptyManager() {
        return (SplitPointDataSource<T>)EMPTY_MANAGER;
    }
    
    @SuppressWarnings("unchecked")
	private static final <T extends SplitPointUnit> Supplements<T> emptySupplements() {
    	return (Supplements<T>)EMPTY_SUPPLEMENTS;
    }
	
	/**
	 * Creates a new instance with the specified units and supplements
	 * @param units the units
	 * @param supplements the supplements
	 */
	public SplitPointDataList(List<T> units, Supplements<T> supplements) {
		this(units, supplements, 0);
	}

	private SplitPointDataList(List<T> units, Supplements<T> supplements, int offset) {
		this.units = units;
		this.offset = offset;
		if (supplements==null) {
			this.supplements = emptySupplements();
		} else {
			this.supplements = supplements;
		}
	}

	@Override
	public Supplements<T> getSupplements() {
		return supplements;
	}

	@Override
	public boolean hasElementAt(int index) {
		return this.units.size()>index+offset;
	}

	@Override
	public boolean isEmpty() {
		return this.units.size()<=offset;
	}

	@Override
	public T get(int n) {
		return this.units.get(offset+n);
	}
	
	public List<T> head(int n) {
		return this.units.subList(offset, offset+n);
	}
	
	public List<T> getRemaining() {
		return this.units.subList(offset, units.size());
	}

	@Override
	public SplitPointDataSource<T> tail(int n) {
		return new SplitPointDataList<T>(units, supplements, offset+n);
	}

	@Override
	public int getSize(int limit) {
		return Math.min(this.units.size()-offset, limit);
	}

}
