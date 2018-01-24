package org.daisy.dotify.common.text;

@FunctionalInterface
public interface ReplaceSequenceMapper {
	public CharSequence replace(char c);
}