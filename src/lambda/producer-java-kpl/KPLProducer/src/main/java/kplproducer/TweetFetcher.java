package kplproducer;

import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.Status;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Query.ResultType;
import twitter4j.ResponseList;
import twitter4j.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;

public class TweetFetcher {
  // Fetches Tweets on demand
  static final int PER_PAGE = 100;

  static final String TWITTER_CONSUMER_KEY = "/twitter/consumer_key";
  static final String TWITTER_CONSUMER_SECRET = "/twitter/consumer_secret";
  static final String TWITTER_ACCESS_KEY = "/twitter/access_token_key";
  static final String TWITTER_ACCESS_SECRET = "/twitter/access_token_secret";

  public static Twitter getTwitterInstance() {
    // Setup a list of parameters to fetch from SSM
    ArrayList<String> parameterSet = new ArrayList<String>();
    parameterSet.add(TWITTER_CONSUMER_KEY);
    parameterSet.add(TWITTER_CONSUMER_SECRET);
    parameterSet.add(TWITTER_ACCESS_KEY);
    parameterSet.add(TWITTER_ACCESS_SECRET);

    // Fetch the parameters as a group
    Map<String, String> twitterCreds = getParameterFromSSMByName(parameterSet);

    // instantiate a cb for the factory
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setJSONStoreEnabled(true);

    // handle OAuth1 setup
    cb.setOAuthConsumerKey(twitterCreds.get(TWITTER_CONSUMER_KEY));
    cb.setOAuthConsumerSecret(twitterCreds.get(TWITTER_CONSUMER_SECRET));
    cb.setOAuthAccessToken(twitterCreds.get(TWITTER_ACCESS_KEY));
    cb.setOAuthAccessTokenSecret(twitterCreds.get(TWITTER_ACCESS_SECRET));

    // set read timeouts
    cb.setHttpReadTimeout(5000);
    cb.setHttpConnectionTimeout(5000);

    // construct the factory and instance
    TwitterFactory factory = new TwitterFactory(cb.build());
    return factory.getInstance();
  }

  public static List<String> queryTweets(Twitter twitter, String trend) {
    // Fetch 100 tweets at a time
    List<String> tweets = new ArrayList<String>(100);
    try {
      // create the query and set the count per page and result_type
      System.out.println("Fetching tweets for trend: " + trend);
      Query query = new Query(trend);
      query.setResultType(ResultType.recent);
      query.setCount(PER_PAGE);
      QueryResult result = twitter.search(query);
      // iterate over the tweets, convert them to JSON strings and store
      List<Status> results = result.getTweets();
      System.out.println("Got tweets, processing...");
      for (Status r : results) {
        tweets.add(TwitterObjectFactory.getRawJSON(r));
      }
      System.out.println("Processed tweets for trend: " + trend);
    } catch (TwitterException te) {
      te.printStackTrace();
      System.out.println("Problem fetching tweets for trend: " + trend);
    }
    return tweets;
  }

  public static Map<String, String> getParameterFromSSMByName(ArrayList<String> parameterKeys) {
    // create our ssm client
    AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();

    // create the request itself
    GetParametersRequest parameterRequest = new GetParametersRequest();
    parameterRequest.withNames(parameterKeys).setWithDecryption(Boolean.valueOf(true));

    // fetch the parameters based on the request
    GetParametersResult parameterResult = ssm.getParameters(parameterRequest);

    // map the parameters by name to a hashmap so we can grab them by key
    Map<String, String> config = new HashMap<>();
    parameterResult.getParameters().forEach(parameter -> {
      config.put(parameter.getName(), parameter.getValue());
    });
    return config;
    }

  public static List<String> getTrends(Twitter twitter, int woeid) {
    // build a list of trends as strings from a given WOEID
    List<String> trend_list = new ArrayList<String>();
    try {
      Trend[] trends = twitter.getPlaceTrends(woeid).getTrends();
      for (Trend trend : trends) {
        trend_list.add(trend.getName());
      }
      return trend_list;
    } catch (TwitterException te) {
      te.printStackTrace();
      System.out.println("Failed to get trends for: " + woeid);
      System.exit(1);
      }
    return null;
    }
  }
