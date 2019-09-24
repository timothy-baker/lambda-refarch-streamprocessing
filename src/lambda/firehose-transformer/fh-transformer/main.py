import json
import base64
import time
import re
import logging
import os
import sys

LOG_LEVEL = os.environ.get("LOG_LEVEL", "").upper()
if LOG_LEVEL == "":
    LOG_LEVEL = "INFO"

logger = logging.getLogger()
logger.setLevel(LOG_LEVEL)

SOURCE_REGEX = '<[^>]*>'

def cut_payload(payload):
    """
    Load the json string, pull the items we want (including any transforms)
    Return a json string ready for base64 encoding
    """
    output_data = {}
    try:
        data = json.loads(payload)
    except Exception as e:
        logger.exception('Failed to read payload for transform')
        raise e
    try:
        output_data['created_at'] = make_timestamp(data['created_at'])
        output_data['id'] = data['id']
        output_data['text'] = data['full_text']
        output_data['user_name'] = data['user']['name']
        output_data['user_screen_name'] = data['user']['screen_name']
        output_data['source'] = re.sub(SOURCE_REGEX, '', data['source'])
        output_data['retweet_count'] = data['retweet_count']
        output_data['favorite_count'] = data['favorite_count']
        output_data['lang'] = data['lang']
        output_data['verified'] = data['user']['verified']
    except KeyError as e:
        logger.error('Record missing data: {}'.format(e))
        raise e
    return json.dumps(output_data)

def make_timestamp(twitter_timestamp_string):
    return time.strftime(
        '%Y-%m-%d %H:%M:%S',
        time.strptime(
            twitter_timestamp_string,
            '%a %b %d %H:%M:%S +0000 %Y'
            )
        )

def lambda_handler(event, context):
    """
    Sample Firehose Record Transformer
    """
    output = []
    encoding = 'utf-8'

    for record in event['records']:
        try:
            record_id = record['recordId']
            logger.info('Processing record: {}'.format(record_id))
            payload = base64.b64decode(record['data'])
            payload_out = base64.b64encode(cut_payload(payload).encode(encoding))

            output_record = {
                'recordId': record_id,
                'result': 'Ok',
                'data': payload_out.decode(encoding)
            }
            output.append(output_record)
            logger.info('Processed record: {}'.format(record_id))
        except Exception as e:
            logger.error('Error : {0}'.format(sys.exc_info()))
            logger.error('Unable to process {}'.format(record_id))
            raise e
    return {'records': output}
    

