package kplproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.math.BigInteger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import kplproducer.TweetFetcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;


public class KPLProducer {
    // This producer is based on the more feature-rich sample producer
    // found at https://github.com/awslabs/amazon-kinesis-producer

    final Logger logger = LoggerFactory.getLogger(KPLProducer.class);

    // We'll use this for our hash key
    private static final Random RANDOM = new Random();

    // These are the parameters we want to collect from environment variables
    private static final String[] config_parameters = {"KINESIS_STREAM_NAME", "REGION", "WOEID"};

    // RecordMaxBufferedTime controls how long records are allowed to wait
    // in the KPL's buffers before being sent. Larger values increase
    // aggregation and reduces the number of Kinesis records put, which can
    // be helpful if you're getting throttled because of the records per
    // second limit on a shard. The default value is set very low to
    // minimize propagation delay, so we'll increase it here to get more
    // aggregation.  For more options see the sample producer at
    // https://github.com/awslabs/amazon-kinesis-producer
    private static final int MAX_BUFFER_TIME = 15000;

    public static String randomExplicitHashKey() {
        // Generates a hash key to use as a partition key
        return new BigInteger(128, RANDOM).toString(10);
    }

    public void handleRequest(final Object input, final Context context) {
        logger.info("KPL Producer Initializing");
        // this is the main method called by lambda at execution
        int recordsProduced = 0;
        Map<String, String> kinesis_config = getEnvVars(config_parameters);
        String STREAM_NAME = kinesis_config.get("KINESIS_STREAM_NAME");
        logger.debug("STREAM_NAME = " + STREAM_NAME);
        String REGION = kinesis_config.get("REGION");
        logger.debug("REGION = " + REGION);
        int WOEID = Integer.parseInt(kinesis_config.get("WOEID"));
        logger.debug("WOEID = " + WOEID);

        // Create our Tweet Fetcher with the given config
        Twitter twitter = TweetFetcher.getTwitterInstance();

        // Get a list of trends to fetch based on the WOEID
        List<String> twitterTrends = TweetFetcher.getTrends(twitter, WOEID, logger);

        // Create a kinesis producer
        final KinesisProducer producer = getKinesisProducer(REGION);

        // Iterate over the tweets and use addUserRecord
        // This method asynchronously aggregates and collects records
        // We generate a random hash to reduce shard heat
        logger.info("KPL Producer Begin Production");
        for (String trend: twitterTrends) {
            List<String> tweetList = TweetFetcher.queryTweets(twitter, trend, logger);
            for(String tweet : tweetList) {
                ByteBuffer data = makeEntry(tweet);
                // doesn't block
                producer.addUserRecord(STREAM_NAME, randomExplicitHashKey(), data);
                recordsProduced++;
            }
        }

        logger.info("Flushing records");
        // Flush any buffered records yet to be sent
        producer.flushSync();
        // Kill the child process and any threads managing it
        producer.destroy();
        String records = Integer.toString(recordsProduced);
        logger.info("Produced " + records + " records.");
    }

    public static KinesisProducer getKinesisProducer(String region) {
        // Create a producer and set some config parameters
        // For more options see the sample producer at
        // https://github.com/awslabs/amazon-kinesis-producer
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();

        // Required if not running on EC2
        config.setRegion(region);
        config.setRecordMaxBufferedTime(MAX_BUFFER_TIME);
        return new KinesisProducer(config);
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
                logger.error("Missing ENV VAR: " + env);
                System.exit(1);
            }
        }
        return kinesis_config;
    }

}
