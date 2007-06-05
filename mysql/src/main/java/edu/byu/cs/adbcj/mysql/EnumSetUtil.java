package edu.byu.cs.adbcj.mysql;

import java.util.EnumSet;

/**
 * Provides methods for converting an EnumSet to a bit vector and back.
 * 
 * @author Mike Heath
 */
public class EnumSetUtil {

	public static <E extends Enum<E>> int toInt(EnumSet<E> s) {
		return (int)toLong(s);
	}
	
	public static <E extends Enum<E>> long toLong(EnumSet<E> s) {
		long vector = 0;
		for (E e : s) {
			vector |= 1 << e.ordinal();
		}
		return vector;
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> enumClass, long vector) {
		EnumSet<E> set = EnumSet.noneOf(enumClass);
		long mask = 1;
		for(E e : enumClass.getEnumConstants()) {
			if ((mask & vector) == mask) {
				set.add(e);
			}
			mask <<= 1;
		}
		return set;
	}
	
}
