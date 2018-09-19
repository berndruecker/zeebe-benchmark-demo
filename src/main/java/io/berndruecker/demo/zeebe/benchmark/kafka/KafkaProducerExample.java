package io.berndruecker.demo.zeebe.benchmark.kafka;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaProducerExample
{
    
    public static void main(String[] args)
    {
        final AtomicLong counter = new AtomicLong();
        new Timer(true)
            .scheduleAtFixedRate(new TimerTask()
            {
                
                @Override
                public void run()
                {
                    long started = counter.getAndSet(0);
                    
                    System.out.println(started);
                }
            }, 1000, 1000);
        
        final byte[] msg = new byte[512];
        
        final Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 1024 * 1024 * 32);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        
        final Producer<String, byte[]> producer = new KafkaProducer<>(props);
        
        final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (endTime > System.currentTimeMillis())
        {
            producer.send(new ProducerRecord<String, byte[]>("default", "test", msg));
            counter.incrementAndGet();
        }
        
        producer.close();
    }
    
}
