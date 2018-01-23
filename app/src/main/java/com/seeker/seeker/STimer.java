package com.seeker.seeker;

/**
 * Created by SooD on 16-05-2017.
 */

public class STimer {

    private long start = 0;

    public long start(){
        start = System.currentTimeMillis();
        return start;
    }
    public long getTime(){
        if(start!=0)
            return System.currentTimeMillis()-start;
        else return -1;
    }
    public void stop(){
        start = 0;
    }



}
