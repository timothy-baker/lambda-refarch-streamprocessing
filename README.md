
# AWS Lambda Reference Architecture: Real-time Stream Processing

You can use AWS Lambda and Amazon Kinesis to process real-time streaming data for application activity tracking, transaction order processing, click stream analysis, data cleansing, metrics generation, log filtering, indexing, social media analysis, and IoT device data telemetry and metering. The architecture described in this [diagram](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-stream-processing.pdf) can be created with an AWS CloudFormation template.

[The template](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-stream-processing.template)
does the following:

-	Creates a VPC (virtual private cloud) named vpc-ref-arch-streamprocessing. (The VPC hosts an Amazon EC2 Windows instance to subscribe to Twitter streaming data.)

-	Creates a subnet named subnet-ref-arch-streamprocessing. (The public subnet hosts the EC2 instance.)

-	Creates a route table named rtb-main-ref-arch-streamprocessing. (The route table is the main route table for the VPC.)

-	Creates a route table named rtb-ref-arch-streamprocessing. (The route table routes the public subnet traffic to the Internet gateway.)

-	Creates an Internet gateway (IGW) named igw-ref-arch-streamprocessing. (The IGW routes the public subnet traffic.)

-	Creates an EC2 Instance named ec2-ref-arch-streamprocessing. (This is a Windows EC2 instance that runs a Windows console application that subscribes to Twitter streaming data.)

-	Creates a security group named sg-ref-arch-streamprocessing. (The security group provides secure access to the EC2 instance.)

-	Creates an IAM role named EC2RoleKinesisFullRefArchStreamProcessing. (This IAM role is assumed by the EC2 instance.)

-	Creates an IAM policy named KinesisPolicyRefArch001StreamProcessing. (This IAM policy is associated with the IAM role named EC2RoleKinesisFullRefArchStreamProcessing that gives necessary permissions to the EC2 instance to add streaming data to the Amazon Kinesis stream.)

-	Creates an IAM role named LambdaKinesisDynamoDBRefArchStreamProcessing. (This IAM role is assumed by the Lambda function named PutTweetsIntoDynamoDBRefArcStreamProcessing.)

-	Creates an IAM policy named KinesisPolicyRefArchStreamProcessing. (This IAM policy is associated to the IAM role LambdaKinesisDynamoDBRefArchStreamProcessing that gives necessary permission to the Lambda function to write data to the Amazon DynamoDB table.)

-	Creates a DynamoDB table named TwitterFeedsRefArchStreamProcessing. (This DynamoDB table stores the Amazon Kinesis streaming data.)

-	Creates an Amazon Kinesis stream named <StackName-StreamRefArchStreamProcessing-<System Generated ID>. (This is the Amazon Kinesis stream that receives the Twitter stream from the Windows EC2 console application.)

-	Creates a Lambda function named PutTweetsIntoDynamoDBRefArcStreamProcessing. (This Lambda function writes to the DynamoDB table.)

-	Creates a Lambda function named MapEventSourceLambdaFunctionRefArchStreamProcessing. (This is an utility Lambda function that adds an event source to the Lambda function named PutTweetsIntoDynamoDBRefArcStreamProcessing.)

-	Creates a Lambda function named AMIInfoFunctionRefArchStreamProcessing. (This Lambda function dynamically looks up the AMI ID used in the EC2 instance.)

-	Creates a custom AWS CloudFormation resource named LookUpAMIRefArchStreamProcessing. (This is a custom AWS CloudFormation resource that invokes the Lambda function named AMIInfoFunctionRefArchStreamProcessing.)

-	Creates a custom AWS CloudFormation resource named MapEventSourceLambdaFxToKinesisStreamRefArchStreamProcessing. (This is a custom AWS CloudFormation resource that invokes the Lambda function named MapEventSourceLambdaFunctionRefArchStreamProcessing.)

## Core Components

The reference architecture has the following core components

-	Windows console application running on a Windows EC2 instance
-	Amazon Kinesis stream
-	Lambda function
-	DynamoDB table (TwitterFeedsRefArchStreamProcessing)

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
