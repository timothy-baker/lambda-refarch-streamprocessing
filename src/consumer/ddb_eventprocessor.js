/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"). You may not use
this file except in compliance with the License. A copy of the License is
located at

http://aws.amazon.com/apache2.0/

or in the "license" file accompanying this file. This file is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and
limitations under the License. */

console.log('Loading function');

const AWS = require('aws-sdk');
const doc = new AWS.DynamoDB.DocumentClient();
const ddbTableName = process.env.DDB_TABLE;

// Set a TTL for DynamoDB records:
// https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/howitworks-ttl.html
const today = new Date();
const expireIn = 2; // days
let expireDate = new Date();
expireDate.setDate(today.getDate() + expireIn);
const expireTimeEpoch = Math.floor(expireDate / 1000);

// Enable of disable the debug logs
const DEBUG = true;
const logger = console.log;
console.log = function() {
  if (DEBUG) logger.apply(this, arguments);
}

/**
 * AWS Lambda function acting as Event Processor for AWS Lambda Stream Processing
 * Reference Architecture
 */
exports.handler = async function(event) {
  console.log(`Received event: ${JSON.stringify(event, null, 2)}`);

  // Unpack the batched items from the event record
  let tableItems = unpackItems(event.Records);

  try {
    // Write the items to DynamoDB
    return await writeItems(tableItems, 0);
  } catch (err) {
    return err;
  }
};

/**
 * This assumes the batch size configured in the the event source mapping
 * is set to a maximum of 25 records. Depending on the characteristics of
 * your system it may make sense to consume larger batches from the stream
 * and manage the batch sizes sent to DynamoDB within the funtion.
 * @param {String} tableName DynamoDB table name
 * @param {[]} records Array of records from the lambda event object
 */
function unpackItems(records) {
  let putItems = [];
  records.forEach(function(record) {
    payload = new Buffer(record.kinesis.data, 'base64').toString('ascii');
    console.log(`Decoded payload: ${payload}`);

    let tweet = JSON.parse(payload);
    console.log(`User: ${tweet.user.name}`);
    console.log(`Timestamp: ${tweet.created_at}`);

    /*
     * Reference the documentation for the DynamoDB Document Client batchWrite method to review the data format:
     * https://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/DynamoDB/DocumentClient.html#batchWrite-property
     */
    putItems.push({
      PutRequest: {
        Item: {
          Username: tweet.user.name,
          Id: tweet.id_str,
          Timestamp: new Date(tweet.created_at.replace(/( \+)/, ' UTC$1')).toISOString(),
          Message: tweet.text,
          ExpirationTime: expireTimeEpoch
        }
      }
    });
  });

  let tableItems = {};
  tableItems[ddbTableName] = putItems;

  return tableItems;
}

/**
 * Use the batchWrite method to persist the records to DynamoDB.
 * @param {Object} items AWS.DynamoDB.DocumentClient batchWrite items
 * @param {Number} retries Number of retries
 */
async function writeItems(items, retries) {
  return new Promise(function(accept, reject) {
    doc.batchWrite({ RequestItems: items }, function (err, data) {
      if (err) {
        console.log(`DDB call failed: ${err}`, err.stack);
        reject(err);
      }
      else {
        // Check for unprocessed items and retry if neccessary
        let unprocessedCount = Object.keys(data.UnprocessedItems).length;
        if(unprocessedCount) {
          console.log(`${unprocessedCount} unprocessed items remain, retrying.`);
          let delay = Math.min(Math.pow(2, retries) * 100, context.getRemainingTimeInMillis() - 200);
          setTimeout(() => writeItems(data.UnprocessedItems, retries + 1), delay);
        } else {
          accept(data);
        }
      };
    });
  });
}
