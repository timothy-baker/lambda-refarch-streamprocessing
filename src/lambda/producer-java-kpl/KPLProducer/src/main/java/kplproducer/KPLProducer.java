package kplproducer;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Future;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordResult;


public class KPLProducer implements RequestHandler<Object, String> {
    public static final String[] config_parameters = {"STREAM_NAME", "REGION", "WOEID"};

    // RecordMaxBufferedTime controls how long records are allowed to wait
    // in the KPL's buffers before being sent. Larger values increase
    // aggregation and reduces the number of Kinesis records put, which can
    // be helpful if you're getting throttled because of the records per
    // second limit on a shard. The default value is set very low to
    // minimize propagation delay, so we'll increase it here to get more
    // aggregation.
    public static final int MAX_BUFFER_TIME = 15000;
    private static final String TIMESTAMP = Long.toString(System.currentTimeMillis());

    public String handleRequest(final Object input, final Context context) {
        int recordsProduced = 0;
        Map<String, String> kinesis_config = getEnvVars(config_parameters);
        String STREAM_NAME = kinesis_config.get("STREAM_NAME");
        String REGION = kinesis_config.get("REGION");
        int WOEID = Integer.parseInt(kinesis_config.get("WOEID"));
        // Fetch some tweets to send to our kinesis stream
        List<String> tweetList = TweetFetcher.getTweets(WOEID);

        // Create a kinesis producer
        final KinesisProducer producer = getKinesisProducer(REGION);

        // Create an LL for our futures
        List<Future<UserRecordResult>> putFutures = new LinkedList<Future<UserRecordResult>>();

        // Iterate over the tweets and use addUserRecord
        // This method asynchronously aggregates and collects records
        // For every run of this lambda, the TIMESTAMP changes reducing shard heat
        for(String tweet : tweetList) {
            ByteBuffer data = makeEntry(tweet);
            putFutures.add(
                // doesn't block
                producer.addUserRecord(STREAM_NAME, TIMESTAMP, data)
            );
            recordsProduced++;
        }

        // we'll wait on the futures/callbacks here
        try {
            for(Future<UserRecordResult> f : putFutures) {
                // blocks until future completes
                // catch exceptions below
                UserRecordResult result = f.get();
                if (result.isSuccessful()) {
                    assert true;
                } else {
                    System.out.println("Record failed to deliver.");
                    recordsProduced--;
                }
            }
        } catch(Exception e) {
            System.out.println("Could not fetch future.");
            recordsProduced--;
        }
        String records = Integer.toString(recordsProduced);
        System.out.println("Produced " + records + " records.");
        return "Processed Stream.";
    }

    public static KinesisProducer getKinesisProducer(String region) {
        // Create a producer and set some config parameters
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();
        config.setRegion(region);
        config.setRecordMaxBufferedTime(MAX_BUFFER_TIME);
        KinesisProducer producer = new KinesisProducer(config);
        return producer;
    }

    public ByteBuffer makeEntry(String tweet) {
        // Create a bytebuffer to send to the kinesis stream
        try {
            return ByteBuffer.wrap(tweet.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getEnvVars (String[] args) {
        // Get some env vars and map them
        Map<String, String> kinesis_config = new HashMap<>();
        for (String env: args) {
            String value = System.getenv(env);
            if (value != null) {
                kinesis_config.put(env, value);
            } else {
                System.out.println("Missing ENV VAR: " + env);
                System.exit(1);
            }
        }
        return kinesis_config;
    }

}
