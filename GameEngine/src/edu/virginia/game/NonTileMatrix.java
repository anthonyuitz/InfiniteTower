package edu.virginia.game;

import java.awt.Point;
import java.util.ArrayList;

public class NonTileMatrix {
	private ArrayList<ArrayList<ArrayList<Integer>>> matrix;

	public NonTileMatrix(int rows, int cols) {
		matrix = new ArrayList<ArrayList<ArrayList<Integer>>>();
		for(int x = 0; x < rows; x++) {
			ArrayList<ArrayList<Integer>> row = new ArrayList<ArrayList<Integer>>();
			for(int y = 0; y < cols; y++) {
				ArrayList<Integer> col = new ArrayList<Integer>();
				row.add(col);
			}
			matrix.add(row);
		}
	}
	
	public void surroundWithEmpty() {
		for(int x = 0; x < matrix.size(); x++) {
			matrix.get(x).add(new ArrayList<Integer>());
			matrix.get(x).add(0, new ArrayList<Integer>());
		}
		ArrayList<ArrayList<Integer>> row = new ArrayList<ArrayList<Integer>>();
		for(int y = 0; y < matrix.get(0).size(); y++) {
			ArrayList<Integer> col = new ArrayList<Integer>();
			row.add(col);
		}
		matrix.add(row);
		ArrayList<ArrayList<Integer>> row2 = new ArrayList<ArrayList<Integer>>();
		for(int y = 0; y < matrix.get(0).size(); y++) {
			ArrayList<Integer> col = new ArrayList<Integer>();
			row2.add(col);
		}
		matrix.add(0, row2);
	}
	
	public ArrayList<ArrayList<Integer>> getRow(int x) {
		return matrix.get(x);
	}
	
	public ArrayList<Integer> get(int x, int y) {
		return matrix.get(x).get(y);
	}
	
	public boolean containsVal(Point p, int val) {
		boolean contains = false;
		ArrayList<Integer> array = get(p);
		for(int x = 0; x < array.size(); x++) {
			if(array.get(x) == val) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	public boolean containsValInRange(Point p, int val, int val2) {
		boolean contains = false;
		ArrayList<Integer> array = get(p);
		for(int x = 0; x < array.size(); x++) {
			if(array.get(x) >= val && array.get(x) <= val2) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	public boolean removeVal(Point p, int val) {
		boolean removed = false;
		ArrayList<Integer> array = get(p);
		for(int x = 0; x < array.size(); x++) {
			if(array.get(x) == val) {
				removed = true;
				matrix.get(p.x).get(p.y).remove(x);
				break;
			}
		}
		return removed;
	}
	
	public boolean addVal(Point p, int val) {
		return matrix.get(p.x).get(p.y).add(val);
	}

	public ArrayList<Integer> get(Point p) {
		return matrix.get(p.x).get(p.y);
	}
	
	public int getVal(int x, int y, int z) {
		return matrix.get(x).get(y).get(z);
	}
	
}
