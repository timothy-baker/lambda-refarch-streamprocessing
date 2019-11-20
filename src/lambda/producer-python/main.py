"""
Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License"). You may not use 
this file except in compliance with the License. A copy of the License is 
located at
http://aws.amazon.com/apache2.0/
or in the "license" file accompanying this file. This file is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing permissions and 
limitations under the License. 
"""

# Python script for reading from Twitter Streaming API and inserting tweets into an Amazon Kinesis Stream

import boto3
import json
import logging
import os
import sys

from TwitterAPI import TwitterAPI, TwitterRequestError

LOG_LEVEL = os.environ.get("LOG_LEVEL", "").upper()
if LOG_LEVEL == "":
    LOG_LEVEL = "INFO"

logger = logging.getLogger()
logger.setLevel(LOG_LEVEL)

# kinesis stream name
try:
    kinesis_stream_name = os.environ['KINESIS_STREAM_NAME']
except Exception as e:
    logger.exception("Environment Variable KINESIS_STREAM_NAME not set")
    raise


def getTwitterCreds():
    ssm = boto3.client('ssm')
    try:
        response = ssm.get_parameters(
            Names=[
                '/twitter/consumer_key',
                '/twitter/consumer_secret',
                '/twitter/access_token_key',
                '/twitter/access_token_secret',
            ],
            WithDecryption=True
        )
        params = {x['Name']: x for x in response['Parameters']}
        creds = {}
        creds['twitter_consumer_key'] = params['/twitter/consumer_key']['Value']
        creds['twitter_consumer_secret'] = params['/twitter/consumer_secret']['Value']
        creds['twitter_access_token_key'] = params['/twitter/access_token_key']['Value']
        creds['twitter_access_token_secret'] = params['/twitter/access_token_secret']['Value']
        return creds
    except:
        print('Problem getting keys from SSM')
        return {
            'statusCode': 501,
            'body': 'Problem getting Twitter credentials from AWS SSM'
        }


# Twitter OAuth Tokens
twitterCreds = getTwitterCreds()
consumer_key = twitterCreds['twitter_consumer_key']
consumer_secret = twitterCreds['twitter_consumer_secret']
access_token_key = twitterCreds['twitter_access_token_key']
access_token_secret = twitterCreds['twitter_access_token_secret']

# Setting up Twitter and Kinesis objects
twitter_api = TwitterAPI(consumer_key, consumer_secret,
                         access_token_key, access_token_secret)
kinesis = boto3.client('kinesis')


def lambda_handler(event, context):
    logger.info('Incoming Event: {0}'.format(event))
    try:
        logger.info('Get top 50 trending topics for given WOED: {0}'.format(
            os.environ['WOEID']))
        top_trend = get_top_trend(os.environ['WOEID'])

        logger.info('Get public statuses for trend {0}'.format(top_trend))
        response = twitter_api.request('statuses/filter', {'track': top_trend})

        logger.info('Putting records to Kinesis stream : {0}'.format(
            kinesis_stream_name))

        # Writes new tweets into Kinesis
        for item in response:
            if 'text' in item:
                kinesis.put_record(StreamName=kinesis_stream_name, Data=json.dumps(
                    item), PartitionKey=item['user']['screen_name'])
                logger.debug(item['text'])

    except TwitterRequestError as e:
        logger.error('TwitterRequestError_Code: {0}'.format(e.status_code))
        logger.error('TwitterRequestError_Details: {0}'.format(
            response.json()['errors'][0]))
    except:
        logger.error('Error : {0}'.format(sys.exc_info()))
        logger.error('Unable to put {0} to stream'.format(item['text']))
        raise Exception('General Exception'.format(sys.exc_info()))

    logger.info('Processing Complete')

    return {
        'statusCode': 200,
        'body': 'Processing Complete'
    }


def get_top_trend(location_id):
    response = twitter_api.request('trends/place', {'id': location_id})
    trends = []
    for item in response.get_iterator():
        trends.append(item)

    # sort all trends by tweet_volume
    trends = sorted(trends, key=lambda i: (i['tweet_volume'] is None, 0))
    if len(trends) > 1:
        top_trend = trends[0]
        return top_trend['name']
    else:
        return '#serverless'
