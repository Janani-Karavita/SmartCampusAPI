package com.smartcampus.model;

public class SensorReading {
    private String id;
    private long timestamp;
    private double value;

    public SensorReading(String id, long timestamp, double value){
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }
    
    public String getId(){
        return id;
    }
    
    public void setId(String id){
        this.id = id;
    }
    
    public long getTimeStamp(){
        return timestamp;
    }
    
    public void setTimeStamp(long timestamp){
        this.timestamp = timestamp;
    }
    
    public double getValue(){
        return value;
    }
    
    public void setValue(double value){
        this.value = value;
    }
}
