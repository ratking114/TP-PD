package server.Threads;


import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

/**
 * TimerThread is a simple thread that sleeps for a determined duration and writes on a pipe
 * when the due time elapses
 */
public class TimerThread extends Thread {
    /**
     * The duration that this Thread shall sleep
     */
    private int _sleep_duration;

    /**
     * The pipe that this Thread writes to when the due time arrives
     */
    private Pipe _write_when_done_pipe;

    private int _timer_id;


    public TimerThread(int timer_id, int sleep_duration, Pipe write_when_done_pipe){
        _sleep_duration = sleep_duration;
        _write_when_done_pipe = write_when_done_pipe;
        _timer_id = timer_id;
    }

    @Override
    public void run() {
        try {
            //sleep for the supplied duration
            Thread.sleep(_sleep_duration);

            //write something on the Pipe since we already slept for the required time
            _write_when_done_pipe.sink().write(ByteBuffer.wrap(" ".getBytes()));
        } catch (Exception ignored) {}
    }

    public int getTimerId(){
        return _timer_id;
    }

    public Pipe getWriteWhenDonePipe(){
        return _write_when_done_pipe;
    }
}
