package hw7;

import java.util.Random;

public class DoubleSkipListThread extends Thread implements ThreadInt{
	private static int THREAD_ID = 0;
	private int id;
	LazyDoubleSkipList<Integer> list;
	private Options option;
	private int limit;
	public volatile int reset;
	private long counter;
	public boolean sink;
	private long addCounter;
	private long remCounter;
	
	public DoubleSkipListThread (LazyDoubleSkipList<Integer> list, Options option, int limit) {
		id = THREAD_ID++;
		this.list = list;
		this.option = option;
		this.limit = limit;
		this.reset = 0;
		this.counter = 0;
		this.addCounter = 0;
		this.remCounter = 0;
	}
	
	public static void setIdZero () {
		THREAD_ID = 0;
	}
	
	void read_0(int t) {
		sink = list.addHead(t);
		if (sink)
			addCounter += 1;
		sink = list.addTail((t + 37) % limit);
		if (sink)
			addCounter += 1;
		sink = list.removeHead((t + 17) % limit);
		if (sink)
			remCounter += 1;
		sink = list.removeTail((t + 23) % limit);
		if (sink)
			remCounter += 1;
		sink = list.addHead((t + 13) % limit);
		if (sink)
			addCounter += 1;
		sink = list.addTail((t * 2) % limit);
		if (sink)
			addCounter += 1;
		sink = list.removeTail((t * 3) % limit);
		if (sink)
			remCounter += 1;
		sink = list.addHead((t + 41) % limit);
		if (sink)
			addCounter += 1;
		sink = list.removeTail((t + 29) % limit);
		if (sink)
			remCounter += 1;
		sink = list.removeHead((t + 4) % limit);
		if (sink)
			remCounter += 1;
	}
	

	void read_80(int t) {
		sink = list.addTail(t);
		if (sink)
			addCounter += 1;
		sink ^= list.containsHead(t);
		sink ^= list.containsTail((t + 23) % limit);
		sink ^= list.containsHead((t * 2) % limit);
		sink ^= list.containsTail((t + 37) % limit);
		sink ^= list.containsHead(t);
		sink ^= list.containsTail((t + 13) % limit);
		sink ^= list.containsHead((t + 1) % limit);
		sink = list.removeHead((t + 41) % limit);
		if (sink)
			remCounter += 1;
		sink ^= list.containsTail((t + 17) % limit);		
	}
	
	@Override
	public void run() {
		int head;
		Random rand = new Random();
		
		switch (option) {
			case READ_80:
				while (this.reset != 1) {
					read_80(rand.nextInt(limit));
					counter+=10;
				}
				break;
			case READ_0:
				while (this.reset != 1) {
					read_0(rand.nextInt(limit));
					counter+=10;
				}
				break;
		}
	}
	
	public int getThreadId(){
		return id;
	}

	public long getCounter() {
		return counter;
	}
	public long getRemCounter() {
		return remCounter;
	}
	
	public long getAddCounter() {
		return addCounter;
	}
}
