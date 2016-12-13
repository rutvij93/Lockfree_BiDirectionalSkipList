package hw7;

import java.util.Random;

public class SingleSkipListThread extends Thread implements ThreadInt{
	private static int THREAD_ID = 0;
	private int id;
	Set<Integer> list;
	private Options option;
	private int limit;
	public volatile int reset;
	private long counter;
	private long addCounter;
	private long remCounter;
	public boolean sink;
	
	public SingleSkipListThread (Set<Integer> list, Options option, int limit) {
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
		sink = list.add(t);
		if (sink)
			addCounter += 1;
		sink = list.add((t + 37) % limit);
		if (sink)
			addCounter += 1;
		sink = list.remove((t + 17) % limit);
		if (sink)
			remCounter += 1;
		sink = list.remove((t + 23) % limit);
		if (sink)
			remCounter += 1;
		sink = list.add((t + 13) % limit);
		if (sink)
			addCounter += 1;
		sink = list.add((t * 2) % limit);
		if (sink)
			addCounter += 1;
		sink = list.remove((t * 3) % limit);
		if (sink)
			remCounter += 1;
		sink = list.add((t + 41) % limit);
		if (sink)
			addCounter += 1;
		sink = list.remove((t + 29) % limit);
		if (sink)
			remCounter += 1;
		sink = list.remove((t + 4) % limit);
		if (sink)
			remCounter += 1;
	}
	

	void read_80(int t) {
		sink = list.add(t);
		if (sink)
			addCounter += 1;
		sink ^= list.contains(t);
		sink ^= list.contains((t + 23) % limit);
		sink ^= list.contains((t * 2) % limit);
		sink ^= list.contains((t + 37) % limit);
		sink ^= list.contains(t);
		sink ^= list.contains((t + 13) % limit);
		sink ^= list.contains((t + 1) % limit);
		sink = list.remove((t + 41) % limit);
		if (sink)
			remCounter += 1;
		sink ^= list.contains((t + 17) % limit);		
	}
	
	@Override
	public void run() {
		
		Random rand = new Random();
		switch (option) {
			case ADD:
				while (this.reset != 1) {
					sink ^= list.add(rand.nextInt(limit));
					counter++;
				}
				break;
			case CONTAINS:
				while (this.reset != 1) {
					sink ^= list.contains(rand.nextInt(limit));
					counter++;
				}
				break;
			case REMOVE:
				while (this.reset != 1) {
					sink ^= list.remove(rand.nextInt(limit));
					counter++;
				}
				break;
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

	public long getRemCounter() {
		return remCounter;
	}
	
	public long getAddCounter() {
		return addCounter;
	}
	
	public long getCounter() {
		return counter;
	}
}
