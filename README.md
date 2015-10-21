
# AWS Lambda Reference Architecture: Real-time Stream Processing

You can use AWS Lambda and Amazon Kinesis to process real-time streaming data for application activity tracking, transaction order processing, click stream analysis, data cleansing, metrics generation, log filtering, indexing, social media analysis, and IoT device data telemetry and metering. The architecture described in this [diagram](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-streamprocessing.pdf) can be created with an AWS CloudFormation template.

[The template](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda_stream_processing.template)
does the following:

-   Creates a Kinesis Stream

-   Creates a DynamoDB table named &lt;stackname&gt;-EventData

-   Creates Lambda Function 1 (&lt;stackname&gt;-IoTDDBEventProcessor)
    which receives records from Kinesis and writes records to the
    DynamoDB table

-   Creates an IAM Role and Policy to allow the event processing Lambda
    function read from the Kinesis Stream and write to the DynamoDB table

## Instructions

Step 1 -  Create an AWS CloudFormation stack with [the
template](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-stream-processing.template) using a lowercase name for the stack. The AWS CloudFormation template completely automates the building, deployment, and configuration of all the components of the application.

Step 2 - Copy the name of the Amazon Kinesis stream once the AWS CloudFormation stack has successfully been completed. You can do so by selecting the Output tab under the stack you created in the AWS CloudFormation management console. You will need the name of the Amazon Kinesis stream in the next step.

Step 3 - Update GetTwitterStream.exe.config in the EC2 instance. RDP into the Windows EC2 instance (ec2-ref-arch-streamprocessing). Go to the folder `C:\aws\download`. In Notepad, open the file GetTwitterStream.exe.config. This is the configuration file for the Windows console application named GetTwitterStream.exe.

Step 4 - Replace the Amazon Kinesis stream name marked as `XXXX` with the actual Amazon Kinesis stream name you copied in step 2. Also replace `XXXX` for the following keys in the configuration files:

- twitterConsumerKey
- twitterConsumerSecret
- userAccessToken
- userAccessTokenSecret
- AWSRegion

## Test

Step 1 - Run the Windows console application GetTwitterStream.exe. Open a command prompt window and change the directory to `C:\aws\download`. Type the following command:

```
C:\aws\download> GetTwitterStream.exe X.
```

X is the number of minutes the program will receive sample tweets from Twitter.

Step 2 - In the Amazon DynamoDB mManagement console, select the table named TwitterFeedsRefArchStreamProcessing and explore the records.

## Cleanup

To remove all created resources, delete the AWS CloudFormation stack.
