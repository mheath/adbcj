package com.taobao.tdhs.config.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;


public class WeightSelector {
	Random ran = new Random();
	
	List<Integer> index = new ArrayList<Integer>();
	
	public Integer getIndex()
	{
		return index.get(ran.nextInt(index.size()));
	}
	public void add(int arg0, Integer arg1) {
		index.add(arg0, arg1);
	}

	public boolean add(Integer arg0) {
		return index.add(arg0);
	}

	public boolean addAll(Collection<? extends Integer> arg0) {
		return index.addAll(arg0);
	}

	public boolean addAll(int arg0, Collection<? extends Integer> arg1) {
		return index.addAll(arg0, arg1);
	}

	public void clear() {
		index.clear();
	}

	public boolean contains(Object arg0) {
		return index.contains(arg0);
	}

	public boolean containsAll(Collection<?> arg0) {
		return index.containsAll(arg0);
	}

	public boolean equals(Object arg0) {
		return index.equals(arg0);
	}

	public Integer get(int arg0) {
		return index.get(arg0);
	}

	public int hashCode() {
		return index.hashCode();
	}

	public int indexOf(Object arg0) {
		return index.indexOf(arg0);
	}

	public boolean isEmpty() {
		return index.isEmpty();
	}

	public Iterator<Integer> iterator() {
		return index.iterator();
	}

	public int lastIndexOf(Object arg0) {
		return index.lastIndexOf(arg0);
	}

	public ListIterator<Integer> listIterator() {
		return index.listIterator();
	}

	public ListIterator<Integer> listIterator(int arg0) {
		return index.listIterator(arg0);
	}

	public Integer remove(int arg0) {
		return index.remove(arg0);
	}

	public boolean remove(Object arg0) {
		return index.remove(arg0);
	}

	public boolean removeAll(Collection<?> arg0) {
		return index.removeAll(arg0);
	}

	public boolean retainAll(Collection<?> arg0) {
		return index.retainAll(arg0);
	}

	public Integer set(int arg0, Integer arg1) {
		return index.set(arg0, arg1);
	}

	public int size() {
		return index.size();
	}

	public List<Integer> subList(int arg0, int arg1) {
		return index.subList(arg0, arg1);
	}

	public Object[] toArray() {
		return index.toArray();
	}

	public <T> T[] toArray(T[] arg0) {
		return index.toArray(arg0);
	}
	
}
