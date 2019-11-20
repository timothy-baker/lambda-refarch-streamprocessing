# Serverless Reference Architecture: Real-time Stream Processing

README Languages: [DE](README/README-DE.md) | [ES](README/README-ES.md) | [FR](README/README-FR.md) | [IT](README/README-IT.md) | [JP](README/README-JP.md) | [KR](README/README-KR.md) |
[PT](README/README-PT.md) | [RU](README/README-RU.md) |
[CN](README/README-CN.md) | [TW](README/README-TW.md)

You can use [AWS Lambda](http://aws.amazon.com/lambda/) and Amazon Kinesis to process real-time streaming data for application activity tracking, transaction order processing, click stream analysis, data cleansing, metrics generation, log filtering, indexing, social media analysis, and IoT device data telemetry and metering.

The [template](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/template.yaml)
creates the following resources:

![Client and Stream Processor Diagram](images/streamprocessing-diagram.png)

- Creates an **Amazon Kinesis Data Stream** for ingesting Tweet records

- Creates an **Amazon Kinesis Data Firehose** for delivery of transformed records to S3 to support Amazon Athena queries

- Creates an **Amazon DynamoDB** table named `<stack-name>-EventData` for storage of parsed and transformed Tweet data

- Creates an **Amazon S3** bucket for storage of transformed records to support Amazon Athena queries

- Creates four **AWS Lambda** functions:

  1. **`<stack-name>-DataStreamConsumer`** which receives records from the Kinesis Data Stream, parses and transforms the records, and writes them to the DynamoDB table
  2. **`<stack-name>-DataStreamProducerJava`** which polls Twitter for trending topics and writes the tweets to the Kinesis Data Stream using the [Kinesis Producer Library](https://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html)
  3. **`<stack-name>-DataStreamProducerPython`** which polls Twitter for trending topics and writes the tweets to the Kinesis Data Stream using the [kinesis.put_record()](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/kinesis.html#Kinesis.Client.put_record) API
  4. **`<stack-name>-FirehoseTransformer`** which transforms records for storage in Amazon S3 and analysis via Amazon Athena queries

- Creates **AWS Identity and Access Management (IAM) Roles** and **Policies** which allow the Lambda functions to interact with the Kinesis Data Stream, Kinesis Firehose Stream, and DynamoDB table

## Instructions

1. To access the Twitter API you need to get [access tokens](https://dev.twitter.com/oauth/overview/application-owner-access-tokens). Make sure you have these available. As a best practice, we are NOT hard coding these credentials in our Lambda functions. Instead, we are using AWS SSM Parameter Store to store them, and get them from within the Lambda code. The Lambda code expects the parameters named as below:

   ```
   /twitter/consumer_key
   /twitter/consumer_secret
   /twitter/access_token_key
   /twitter/access_token_secret
   ```

   You can add these parameters manually by going to AWS Systems Manager > Parameter Store on the AWS Web Management Console, or by running the below commands on the AWS CLI. Note: make sure the credentials you are using in your CLI are allowed to perform the `ssm put-parameter` API call. For more information on setting up your IAM user permissions for Systems Manager Parameters, see here: [Control Access to Systems manager Parameters](https://docs.aws.amazon.com/systems-manager/latest/userguide/sysman-paramstore-access.html)

   ```
   aws ssm put-parameter --name "/twitter/consumer_key" --value "xxx" --type "SecureString"
   aws ssm put-parameter --name "/twitter/consumer_secret" --value "yyy" --type "SecureString"
   aws ssm put-parameter --name "/twitter/access_token_key" --value "zzz" --type "SecureString"
   aws ssm put-parameter --name "/twitter/access_token_secret" --value "jjj" --type "SecureString"
   ```

2. Build and Deploy the Application Stack

   You may deploy the application stack using either method below:

   1. Launch the AWS CloudFormation stack with [the
      template](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/template.yaml).

      The AWS CloudFormation template completely automates the building, deployment, and configuration of all the components of the application.

      [![Launch Real-time Stream Processing into North Virginia with CloudFormation](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/images/cloudformation-launch-stack-button.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=lambda-refarch-streamprocessing&templateURL=https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/template.yaml)

   2. Using SAM CLI

      Install (or upgrade) the AWS SAM CLI

      ```bash
      pip install --upgrade pip --user
      hash -r
      pip install --upgrade aws-sam-cli --user
      ```

      Deploy the stack:

      ```bash
      sam build && sam package --s3-bucket <your-bucket-name> --output-template-file packaged.yaml --region us-east-1
      sam deploy --template-file ./packaged.yaml --stack-name <stack-name> --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND
      ```

## Validation

1. `TODO: instructions for Kinesis analytics`

2. In the **Amazon DynamoDB** management console, select the table named `<stack-name>-EventData` and explore the records.

3. `TODO: instructions for S3 / Athena`

4. `TODO: CloudWatch Logs?`

## Cleanup

To remove all created resources, delete the AWS CloudFormation stack. You will receive an error that the S3 bucket is not empty. Navigate to the S3 console, select the bucket and choose the option to empty the bucket. Return to the CloudFormation console and re-try deleting the stack.
