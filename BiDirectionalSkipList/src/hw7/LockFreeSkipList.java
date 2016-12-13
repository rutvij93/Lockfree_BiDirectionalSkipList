package hw7;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicMarkableReference;

public final class LockFreeSkipList<T> implements Set<T>{
    static final int MAX_LEVEL = 64;
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);
    
    public LockFreeSkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
        }
    }
    
    private int randomLevel() {
    	ThreadLocalRandom  random = ThreadLocalRandom.current();    
    	double max = Math.pow(2, MAX_LEVEL);
    	double min = Math.pow(2,  -1 * MAX_LEVEL);
        double candidate = (random.nextDouble(min, 1) * max);
        return (int) (MAX_LEVEL - Math.log10(candidate) / Math.log10(2));
    }
    
    public long size () {
    	long count = 0;
    	Node<T> tmp = head.next[0].getReference();
    	
    	while (tmp != tail) {
    		tmp = tmp.next[0].getReference();
    		count++;
    	}
    	return count;
    }
    
    public boolean add(T x) {
        int topLevel = randomLevel();
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        while (true) {
            boolean found = find(x, preds, succs);
            if (found) {
                return false;
            } else {
                Node<T> newNode = new Node<T>(x, topLevel);
                
                for (int level = 0; level <= topLevel; level++) {
                	 Node<T> succ = succs[level];
                	 newNode.next[level].set(succ, false);
                }
                
                Node<T> pred = preds[0];
                Node<T> succ = succs[0];
                
                if (!pred.next[0].compareAndSet(succ, newNode,
                            false, false)) {
                    continue;
                }
                for (int level = 1; level <= topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs);
                    }
                }
                return true;
            }
        }
    }
    
    public boolean remove(T x) {
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        
        while (true) {
            boolean found = find(x, preds, succs);
            if (!found) {
                return false;
            } else {
                Node<T> nodeToRemove = succs[0];
                boolean[] marked = {false};
                for (int level = nodeToRemove.topLevel; level >= 1; level--) {
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                marked[0] = false;
                succ = nodeToRemove.next[0].get(marked);
                while (true) {
                    boolean iMarkedIt = nodeToRemove.next[0].compareAndSet(succ, succ, false, true);
                    succ = nodeToRemove.next[0].get(marked);
                    if (iMarkedIt) {
                        find(x, preds, succs);
                        return true;
                    }
                    else if (marked[0]) {
                    	return false;
                    }
                }
            }
        }
    }

    boolean find(T x, Node<T>[] preds, Node<T>[] succs) {
        int key = x.hashCode();
        boolean[] marked = {false};
        boolean snip;
        Node<T> pred = null, curr = null, succ = null;
    retry:
        while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= 0; level--) {
                curr = pred.next[level].getReference();
                Node<T> oldCurr;
                while (true) {
                    succ = curr.next[level].get(marked);
                    oldCurr = curr;
                     while (marked[0]) {
                    	 curr = succ;
                    	 succ = curr.next[level].get(marked);
                     }
                     
                     if (curr != oldCurr) {
                    	 snip = pred.next[level].compareAndSet(oldCurr, curr, false, false);
                    	 if (!snip) {
                    		 continue retry;
                    	 }
                     }
                     
                    if (curr.key < key){
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return (curr.key == key);
        }
    }

    public boolean contains(T x) {
        int v = x.hashCode();
        boolean[] marked = {false};
        Node<T> pred = head;
        Node<T> curr = null;
        Node<T> succ = null;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            curr = pred.next[level].getReference();
            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {
                    curr = succ;
                    succ = curr.next[level].get(marked);
                }
                if (curr.key < v){
                    pred = curr;
                    curr = succ;
                } else {
                    break;
                }
            }
        }
        return (curr.key == v);
    }

    public static final class Node<T> {
        final T value;
        final int key;
        final AtomicMarkableReference<Node<T>>[] next;
        int topLevel;

        public Node(int keyVal) {
            value = null;
            this.key = keyVal;
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null,false);
            }
            topLevel = MAX_LEVEL;
        }

        public Node(T x, int height) {
            value = x;
            key = x.hashCode();
            next = (AtomicMarkableReference<Node<T>>[])new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null,false);
            }
            topLevel = height;
        }
    }
}