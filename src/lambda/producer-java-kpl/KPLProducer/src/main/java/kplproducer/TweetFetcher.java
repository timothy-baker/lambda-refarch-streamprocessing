package kplproducer;

import twitter4j.Trend;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.Status;
// import twitter4j.auth.AccessToken;
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

  static final int ITERATIONS = 5;
  static final int PER_PAGE = 100;

  static final String TWITTER_CONSUMER_KEY = "/twitter/consumer_key";
  static final String TWITTER_CONSUMER_SECRET = "/twitter/consumer_secret";
  static final String TWITTER_ACCESS_KEY = "/twitter/access_token_key";
  static final String TWITTER_ACCESS_SECRET = "/twitter/access_token_secret";

  public static List<String> getTweets(int woeid) {
    // set up the parameters we want to fetch
    ArrayList<String> parameterSet = new ArrayList<String>();
    parameterSet.add(TWITTER_CONSUMER_KEY);
    parameterSet.add(TWITTER_CONSUMER_SECRET);
    parameterSet.add(TWITTER_ACCESS_KEY);
    parameterSet.add(TWITTER_ACCESS_SECRET);

    // fetch the parameters as a group
    Map<String, String> twitterCreds = getParameterFromSSMByName(parameterSet);

    // pass the parameters to the queryTweets method
    List<String> tweets = queryTweets(twitterCreds, woeid);

    return tweets;
  }

  public static List<String> queryTweets(Map<String, String> twitterCreds, int woeid) {
    // instantiate a cb for the factory
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setJSONStoreEnabled(true);

    // handle OAuth1 setup
    // AccessToken accessToken = new AccessToken(twitterCreds.get(TWITTER_ACCESS_KEY), twitterCreds.get(TWITTER_ACCESS_SECRET));
    cb.setOAuthConsumerKey(twitterCreds.get(TWITTER_CONSUMER_KEY));
    cb.setOAuthConsumerSecret(twitterCreds.get(TWITTER_CONSUMER_SECRET));
    cb.setOAuthAccessToken(twitterCreds.get(TWITTER_ACCESS_KEY));
    cb.setOAuthAccessTokenSecret(twitterCreds.get(TWITTER_ACCESS_SECRET));

    // construct the factory and instance
    TwitterFactory factory = new TwitterFactory(cb.build());
    Twitter twitter = factory.getInstance();

    // create a List to hold JSON tweet strings
    // 50 trends * 100 tweets (at max)
    List<String> tweets = new ArrayList<String>(5000);

    try {
      List<String> trends = getTrends(twitter, woeid);
      // iterate over the trends and use them as search queries
      for (String trend : trends) {
      // create the query and set the count per page and result_type
        System.out.println("Fetching tweets for trend: " + trend);
        Query query = new Query(trend);
        query.setResultType(ResultType.recent);
        query.setCount(PER_PAGE);
        QueryResult result;

        result = twitter.search(query);
        // iterate over the tweets, convert them to JSON strings and store
        List<Status> results = result.getTweets();
        for (Status r : results) {
          tweets.add(TwitterObjectFactory.getRawJSON(r));
        }
      }
    } catch (TwitterException te) {
      te.printStackTrace();
      System.out.println("Problem fetching tweets");
      System.exit(1);
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
