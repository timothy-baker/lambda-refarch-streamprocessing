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

from TwitterAPI import TwitterAPI

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

# Twitter OAuth Tokens
consumer_key = ""
consumer_secret = ""
access_token_key = ""
access_token_secret = ""

# Setting up Twitter and Kinesis objects
api = TwitterAPI(consumer_key, consumer_secret, access_token_key, access_token_secret)
kinesis = boto3.client('kinesis')

def lambda_handler(event,context):
    r = api.request('statuses/filter', {'track': 'yolo'})
    # Writes new tweets into Kinesis
    for item in r:
        if 'text' in item:
            try:
                kinesis.put_record(StreamName=kinesis_stream_name, Data=json.dumps(item), PartitionKey=item['user']['screen_name'])
                #print (item['text'])
            except:
                logger.error('Unable to Put record in kinesis : {0}'.format(sys.exc_info()) )
                raise Exception('Error: '.format(sys.exc_info()))
    return 'processing complete'

