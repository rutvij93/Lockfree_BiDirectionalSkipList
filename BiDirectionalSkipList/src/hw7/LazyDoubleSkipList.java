package hw7;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LazyDoubleSkipList<T> {
    static final int MAX_LEVEL = 64;
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);
    
    public LazyDoubleSkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = tail;
            tail.prev[i] = head;
        }
    }
    
    public long size () {
    	long count = 0;
    	Node<T> tmp = head.next[0];
    	
    	while (tmp != tail) {
    		tmp = tmp.next[0];
    		count++;
    	}
    	return count;
    }
    
    int findHead(T x, Node<T>[] preds, Node<T>[] succs) {
    	int key = x.hashCode();
    	int lFound = -1;
    	Node<T> pred = head;
    	for (int level = MAX_LEVEL; level >= 0; level--) {
    		Node<T> curr = pred.next[level];
    		while (key > curr.key) {
    			pred = curr;
    			curr = pred.next[level];
    		}
    		if (lFound == -1 && key == curr.key) {
    			lFound = level;
    		}
    		preds[level] = pred;
    		succs[level] = curr;
    	}
    	return lFound;
    }

    int findTail(T x, Node<T>[] preds, Node<T>[] succs) {
    	int key = x.hashCode();
    	int lFound = -1;
    	Node<T> pred = tail;
    	for (int level = MAX_LEVEL; level >= 0; level--) {
    		Node<T> curr = pred.prev[level];
    		while (key <= curr.key) {
    			pred = curr;
        		if (lFound == -1 && key == curr.key) {
        			lFound = level;
        		}
    			curr = pred.prev[level];
    		}

    		preds[level] = pred;
    		succs[level] = curr;
    	}
    	return lFound;
    }    
    
    public boolean addHead(T x) {
    	int topLevel = randomLevel();
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	while (true) {
    		int lFound = findHead(x, preds, succs);
    		if (lFound != -1) {
    			Node<T> nodeFound = succs[lFound];
    			if (!nodeFound.marked) {
    				while (!nodeFound.fullyLinked) {};
    				return false;
    			}
    			continue;
    		}
    		int highestLocked = -1;
    		try {
    			Node<T> pred, succ;
    			boolean valid = true;
    			for (int level = 0; valid && (level <= topLevel); level++) {
    				pred = preds[level];
    				succ = succs[level];
    				pred.lock();
    				highestLocked = level;
    				valid = !pred.marked && !succ.marked && pred.next[level]==succ && succ.prev[level] == pred;
    			}
    			if (!valid) continue;
    			Node<T> newNode = new Node<T>(x, topLevel);
    			for (int level = 0; level <= topLevel; level++) {
    				newNode.next[level] = succs[level];
    				newNode.prev[level] = preds[level];
    			}
    			for (int level = 0; level <= topLevel; level++) {
    				preds[level].next[level] = newNode;
    				succs[level].prev[level] = newNode;
    			}
    			newNode.fullyLinked = true;
    			return true;
    		} finally {
    			for (int level = 0; level <= highestLocked; level++)
    				preds[level].unlock();
    		}
    	}
    }    

    public boolean addTail(T x) {
    	int topLevel = randomLevel();
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	
    	while (true) {
    		int lFound = findTail(x, preds, succs);
    		if (lFound != -1) {
    			Node<T> nodeFound = preds[lFound];
    			if (!nodeFound.marked) {
    				while (!nodeFound.fullyLinked) {};
    				return false;
    			}
    			continue;
    		}
    		int highestLocked = -1;
    		try {
    			Node<T> pred, succ;
    			boolean valid = true;
    			for (int level = 0; valid && (level <= topLevel); level++) {
    				pred = preds[level];
    				succ = succs[level];
    				succ.lock();
    				highestLocked = level;
    				valid = !pred.marked && !succ.marked && pred.prev[level]==succ && succ.next[level] == pred;
    			}
    			if (!valid) continue;
    			Node<T> newNode = new Node<T>(x, topLevel);
    			for (int level = 0; level <= topLevel; level++) {
    				newNode.next[level] = preds[level];
    				newNode.prev[level] = succs[level];
    			}
    			for (int level = 0; level <= topLevel; level++) {
    				succs[level].next[level] = newNode;
    				preds[level].prev[level] = newNode;
    			}
    			newNode.fullyLinked = true; 
    			return true;
    		} finally {
    			for (int level = 0; level <= highestLocked; level++)
    				succs[level].unlock();
    		}
    	}
    }        

    public boolean removeHead(T x) {
    	Node<T> victim = null;
    	boolean isMarked = false;
    	int topLevel = -1;
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	
    	while (true) {
    		int lFound = findHead(x, preds, succs);
    		if (lFound != -1) victim = succs[lFound];
    		
    		if (isMarked ||
    				(lFound != -1 &&
    				(victim.fullyLinked
    						&& victim.topLevel == lFound
    						&& !victim.marked))) {
    			if (!isMarked) {
    				topLevel = victim.topLevel;
    				victim.lock();
    				if (victim.marked) {
    					victim.unlock();
    					return false;
    				}
    				victim.marked = true;
    				isMarked = true;
    			}
    			int highestLocked = -1;
    			try {
    				Node<T> pred;
    				boolean valid = true;
    				for (int level = 0; valid && (level <= topLevel); level++) {
    					pred = preds[level];
    					pred.lock();
    					highestLocked = level;
    					valid = !pred.marked && pred.next[level]==victim && victim.prev[level]==pred;
    				}
    				if (!valid) continue;

    				for (int level = topLevel; level >= 0; level--) {
    					preds[level].next[level] = victim.next[level];
    					victim.next[level].prev[level] = preds[level]; 
    				}
    				victim.unlock();
    				return true;
    			} finally {
    				for (int i = 0; i <= highestLocked; i++) {
    					preds[i].unlock();
    				}
    			}
    		} else return false;
    	}
    }    

    public boolean removeTail(T x) {
    	Node<T> victim = null;
    	boolean isMarked = false;
    	int topLevel = -1;
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	while (true) {
    		int lFound = findTail(x, preds, succs);
    		if (lFound != -1) victim = preds[lFound];
    		if (isMarked ||
    				(lFound != -1 &&
    				(victim.fullyLinked
    						&& victim.topLevel == lFound
    						&& !victim.marked))) {
    			if (!isMarked) {
    				topLevel = victim.topLevel;
    				victim.lock();
    				if (victim.marked) {
    					victim.unlock();
    					return false;
    				}
    				victim.marked = true;
    				isMarked = true;
    			}
    			int highestLocked = -1;
    			try {
    				Node<T> succ;
    				boolean valid = true;
    				for (int level = 0; valid && (level <= topLevel); level++) {
    					succ = succs[level];
    					succ.lock();
    					highestLocked = level;
    					valid = !succ.marked && succ.next[level]==victim && victim.prev[level]== succ;
    				}
    				if (!valid) continue;

    				for (int level = topLevel; level >= 0; level--) {
    					succs[level].next[level] = victim.next[level];
    					victim.next[level].prev[level] = succs[level]; 
    				}
    				victim.unlock();
    				return true;
    			} finally {
    				for (int i = 0; i <= highestLocked; i++) {
    					succs[i].unlock();
    				}
    			}
    		} else return false;
    	}
    }    

    public boolean containsHead(T x) {
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	int lFound = findHead(x, preds, succs);
    	return (lFound != -1
    			&& succs[lFound].fullyLinked
    			&& !succs[lFound].marked);
    }

    public boolean containsTail(T x) {
    	Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
    	int lFound = findTail(x, preds, succs);
    	return (lFound != -1
    			&& preds[lFound].fullyLinked
    			&& !preds[lFound].marked);
    }
    
    private int randomLevel() {
    	ThreadLocalRandom  random = ThreadLocalRandom.current();    
    	double max = Math.pow(2, MAX_LEVEL);
    	double min = Math.pow(2,  -1 * MAX_LEVEL);
        double candidate = (random.nextDouble(min, 1) * max);
        return (int) (MAX_LEVEL - Math.log10(candidate) / Math.log10(2));
    }    
    
    private static final class Node<T> {
    	final Lock lock = new ReentrantLock();
    	final T item;
    	final int key;
    	volatile Node<T>[] next;
    	volatile Node<T>[] prev;
    	volatile boolean marked = false;
    	volatile boolean fullyLinked = false;
    	private int topLevel;
    	public Node(int key) {
    		this.item = null;
    		this.key = key;
    		next = new Node[MAX_LEVEL + 1];
    		prev = new Node[MAX_LEVEL + 1];
    		topLevel = MAX_LEVEL;
    	}
    	public Node(T x, int height) {
    		item = x;
    		key = x.hashCode();
    		next = new Node[height + 1];
    		prev = new Node[height + 1];
    		topLevel = height;
    	}
    	public void lock() {
    		lock.lock();
    	}
    	public void unlock() {
    		lock.unlock();
    	}
    }
}