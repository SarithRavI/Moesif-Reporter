package moesif.analytics.client;

public class ClientContextHolder {
    public static ThreadLocal<Integer> publishAttempts = new ThreadLocal<Integer>(){
        @Override
        protected Integer initialValue() {
            return new Integer(1);
        }
        @Override
        public Integer get(){
            return super.get();
        }
        @Override
        public void set(Integer value) {
            super.set(value);
        }
    };
}
