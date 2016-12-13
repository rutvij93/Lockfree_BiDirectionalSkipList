package hw7;


public class DoubleSkipListTest {
	
	static final int THREAD_COUNT = 64;
	static final Options OPTION = Options.READ_0;
	static final int LIMIT = 100;

	public static void runTest (int threadCount, Options option, int limit) throws InterruptedException
	{
		long total = 0;
		long addTotal = 0;
		long remTotal = 0;
		long left = 0;
		// Below is for warmup
		for (int k = 0; k < 5; k++) {
			LazyDoubleSkipList<Integer> list = new LazyDoubleSkipList<Integer>();
			total = 0;
			addTotal = 0;
			remTotal = 0;
			left = 0;
			
			final DoubleSkipListThread[] threads1 = new DoubleSkipListThread[threadCount];

			for(int t=0; t<threadCount; t++) {
				threads1[t] = new DoubleSkipListThread(list, option, limit);
			}

			DoubleSkipListThread.setIdZero();
			for(int t=0; t<threadCount; t++)
				threads1[t].start();

			Thread.sleep(1000);
			for(int t=0; t<threadCount; t++) {
				threads1[t].reset = 1;
				threads1[t].join();
				total += threads1[t].getCounter();
				addTotal += threads1[t].getAddCounter();
				remTotal += threads1[t].getRemCounter();
			}	
			left = list.size();
			
		}
		System.out.println("Added->" + addTotal + " Removed + left->" + (remTotal + left));
		System.out.println("Throughput for " + option + " on " + threadCount + " threads is " + total + " Op/sec");
	}
	
	public static void main(String[] args) throws InterruptedException {
			runTest (THREAD_COUNT, OPTION, LIMIT);
	}
}
