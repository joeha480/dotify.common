package org.daisy.dotify.common.layout;

import java.util.List;

public final class SplitPointHandlerSpecification<T extends SplitPointUnit> {
	private final SplitPointCost<T> cost;
	public static class Builder<T extends SplitPointUnit> {
		
		boolean force = false;
		boolean trimLeading = true;
		private SplitPointCost<T> cost;
		
		public Builder(double splitPoint, SplitPointData<T> data) {
			
		}
		
		public SplitPointHandlerSpecification<T> build() {
			return new SplitPointHandlerSpecification<T>(this);
		}

	}

	@SafeVarargs
	public static <T extends SplitPointUnit> Builder<T> with(double splitPoint, T ... units) {
		return new SplitPointHandlerSpecification.Builder<T>(splitPoint, new SplitPointData<>(units));
	}

	public static <T extends SplitPointUnit> Builder<T> with(double splitPoint, List<T> units) {
		return new SplitPointHandlerSpecification.Builder<T>(splitPoint, new SplitPointData<>(units));
	}

	public static <T extends SplitPointUnit> Builder<T> with(double splitPoint, SplitPointData<T> data) {
		return new SplitPointHandlerSpecification.Builder<T>(splitPoint, data);
	}

	private SplitPointHandlerSpecification(Builder<T> builder) {
		if (builder.cost==null) {
			this.cost = new SplitPointCost<T>() {
					@Override
					public double getCost(List<T> units, int breakpoint) {
						// 1. the smaller the result, the higher the cost
						// 2. breakable units are always preferred over forced ones
						return (units.get(breakpoint).isBreakable()?1:2)*units.size()-breakpoint;
					}
				}; 
		} else {
			this.cost = builder.cost;
		}
	}

}
 