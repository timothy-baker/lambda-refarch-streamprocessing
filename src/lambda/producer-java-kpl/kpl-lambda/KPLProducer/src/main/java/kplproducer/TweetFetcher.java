package kplproducer;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.Status;
import twitter4j.auth.AccessToken;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Query.ResultType;

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

  public static List<List<Status>> getTweets(String query) {
    // set up the parameters we want to fetch
    ArrayList<String> parameterSet = new ArrayList<String>();
    parameterSet.add(TWITTER_CONSUMER_KEY);
    parameterSet.add(TWITTER_CONSUMER_SECRET);
    parameterSet.add(TWITTER_ACCESS_KEY);
    parameterSet.add(TWITTER_ACCESS_SECRET);

    // fetch the parameters as a group
    Map<String, String> twitterCreds = getParameterFromSSMByName(parameterSet);

    // pass the parameters to the queryTweets method
    List<List<Status>> tweets = queryTweets(query, twitterCreds);

    return tweets;
  }

  public static List<List<Status>> queryTweets(String queryString, Map<String, String> twitterCreds) {
    // instantiate a factory
    TwitterFactory factory = new TwitterFactory();
    Twitter twitter = factory.getInstance();

    // handle OAuth1 setup
    AccessToken accessToken = new AccessToken(twitterCreds.get(TWITTER_ACCESS_KEY), twitterCreds.get(TWITTER_ACCESS_SECRET));
    twitter.setOAuthConsumer(twitterCreds.get(TWITTER_CONSUMER_KEY), twitterCreds.get(TWITTER_CONSUMER_SECRET));
    twitter.setOAuthAccessToken(accessToken);

    // create a List of Lists to hold paginated results
    List<List<Status>> tweets = new ArrayList<List<Status>>(100);

    try {
      // create the query and set the count per page and result_type
      Query query = new Query(queryString);
      query.setResultType(ResultType.recent);
      query.setCount(PER_PAGE);
      QueryResult result;

      // Iterate for a pre-defined number of iterations.
      // This allows us to let the producer send records
      // in a secondary step where we aren't waiting for
      // the tweet API to respond between puts
      for(int i = 0; i < ITERATIONS+1; ++i) {
        result = twitter.search(query);
        tweets.add(result.getTweets());
        // if there are no more results just break out of the loop
        if (result.hasNext()) {
          query = result.nextQuery();
        } else {
          System.out.println("Max query reached");
          break;
        }
      }
    } catch (TwitterException te) {
      te.printStackTrace();
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

    // map the parameters by name to a hashmap
    // so we can grab them by key
    Map<String, String> config = new HashMap<>();
    parameterResult.getParameters().forEach(parameter -> {
      config.put(parameter.getName(), parameter.getValue());
    });
    return config;
    }
  }