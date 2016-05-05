
# Serverless Reference Architecture: Real-time Stream Processing

The Real-time Stream Processing reference architecture ([diagram](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-streamprocessing.pdf)) demonstrates how to use [AWS Lambda](http://aws.amazon.com/lambda/) and Amazon Kinesis to process real-time streaming data for application activity tracking, transaction order processing, click stream analysis, data cleansing, metrics generation, log filtering, indexing or social media analysis. Using this architecture you can build solutions for these use cases that are easy to maintain and cost-efficient at any scale.

This repository contains sample code for both Lambda functions depicted in this [diagram](https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/lambda-refarch-streamprocessing.pdf) as well as an AWS CloudFormation template for creating the functions and related resources. There is also a script for ingesting Twitter feed data into the system.


## Running the Example

The entire example system can be deployed in us-east-1 using the provided CloudFormation template and an S3 bucket. If you would like to deploy the template to a different region, you must copy the Lambda deployment packages under the `stream-processing` prefix in the `awslambda-reference-architectures` bucket to a new S3 bucket in your target region. You can then provide this new bucket as a parameter when launching the template.

Choose **Launch Stack** to launch the template in the us-east-1 region in your account:

[![Launch Lambda Stream Processor into North Virginia with CloudFormation](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/images/cloudformation-launch-stack-button.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=lambda-stream-processing&templateURL=https://s3.amazonaws.com/awslambda-reference-architectures/stream-processing/stream-processing.template)

## Testing the Example

You can use the provided twitter2kinesis.py script to stream live tweets into the example Kinesis stream.

1. Ensure you have Python 2.7 installed on your local system.
1. Obtain an OAuth access token fromt the dev.twitter.com application control panel. Follow the instructions [here](https://dev.twitter.com/oauth/overview/application-owner-access-tokens) to complete this step.
1. Update the values for the variables defined in the Twitter OAuth Tokens section of the twitter2kinesis.py script based on the token generated in the previous step.
1. Update the AWS credentials, region and stream_name variables in the twitter2kinesis.py script using the outputs of your CloudFormation stack.
1. Run the script by typing `python twitter2kinesis.py` on the command line of your local system.

While the script is running you can check the `EventDataTable` using the DynamoDB console to see the tweets being persisted by your Lambda stream processing function.

After verifying the table, terminate the twitter2kinesis.py script.

## Cleaning Up the Example Resources

To remove all resources created by this example, do the following:

1. Delete the CloudFormation stack.
1. Delete all CloudWatch log groups for each of the Lambda functions in the stack.

## CloudFormation Template Resources

The following sections explain all of the resources created by the CloudFormation template provided with this example.

### Lambda functions

- **DdbEventProcessorFunction** - A Lambda function that processes events from `EventStream` and persists them to both the `EventDataTable`.

### Function roles

- **EventProcessorRole** - An IAM role assumed by the `DdbEventProcessorFunction`. This role provides permissions for logging, writing items to `EventDataTable`. It also provides permissions for the function to get items from `ConfigTable` in order to read configuration data from the function's description.

### Event source mappings

- **DdbEventProcessorSourceMapping** - An event source mapping that enables `DdbEventProcessorFunction` to process records from `EventStream`.

### IAM Users and Policies

- **TestClientUser** - An IAM user used by the test script.

- **TestClientPolicy** - An IAM policy attached to `TestClientUser` that grants access to put records on the `EventStream`.

- **TestClientKeys** - Access keys that enable the test script to sign API requests in order to put records on the `EventStream`.


### Other Resources

- **EventStream** - An Amazon Kinesis stream to receive the raw sensor data.

- **EventDataTable** - A DynamoDB table to store the processed tweet data.

### Configuration

- **ConfigTable** - A DynamoDB table to hold configuration data read by `DdbEventProcessorFunction`. The name of this table, "StreamProcessingRefArchConfig", is hard coded into the function's code and cannot be modified without updating the code as well.

- **ConfigHelperStack** - A sub-stack that creates a custom resource for writing entries to the `ConfigTable`. This stack creates a Lambda function and execution role that grants UpdateItem permission on the `ConfigTable`.

- **EventDataTableConfig** - A configuration entry that identifies the `EvenDataTable` name.

## License

This reference architecture sample is licensed under Apache 2.0.
