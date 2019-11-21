// const response = require("./cfn-response");
const AWS = require("aws-sdk");
const request = require("request");
const lambda = new AWS.Lambda();

function invokeLambda(FunctionName) {
  const params = {
    FunctionName,
    InvokeArgs: JSON.stringify({})
  };
  return new Promise((accept, reject) => {
    lambda.invokeAsync(params, function(err, res) {
      if (err) {
        console.log(`Error: ${JSON.stringify(err, null, 2)}`);
        reject({ Error: "Invoke call failed" });
      } else accept();
    });
  });
}

function sendResponse(
  event,
  context,
  responseStatus,
  responseData,
  physicalResourceId,
  noEcho
) {
  const responseBody = JSON.stringify({
    Status: responseStatus,
    Reason:
      "See the details in CloudWatch Log Stream: " + context.logStreamName,
    PhysicalResourceId: physicalResourceId || context.logStreamName,
    StackId: event.StackId,
    RequestId: event.RequestId,
    LogicalResourceId: event.LogicalResourceId,
    NoEcho: noEcho || false,
    Data: responseData
  });

  console.log("Response body:\n", responseBody);

  const options = {
    method: "PUT",
    url: event.ResponseURL,
    body: responseBody,
    headers: {
      "Content-Type": ""
    }
  };

  return new Promise((resolve, reject) => {
    request(options, (err, httpResponse) => {
      if (err) {
        console.log("send(..) failed executing request.put(..): " + err);
        reject(err);
      } else {
        console.log(`httpResponse: ${httpResponse}`);
        console.log(
          `httpResponse stringify: ${JSON.stringify(httpResponse, null, 2)}`
        );
        resolve();
      }
    });
  });
}

exports.handler = async (event, context) => {
  console.log("REQUEST RECEIVED:\n" + JSON.stringify(event));

  if (event.RequestType == "Delete" || event.RequestType == "Update") {
    // response.send(event, context, response.SUCCESS, {});
    await sendResponse(event, context, "SUCCESS", {});
  }

  if (event.RequestType == "Create") {
    try {
      await invokeLambda(event.ResourceProperties.ProducerJavaArn);
      await invokeLambda(event.ResourceProperties.ProducerPythonArn);
      // response.send(event, context, response.SUCCESS, {});
      await sendResponse(event, context, "SUCCESS", {});
    } catch (err) {
      // response.send(event, context, response.FAILED, err);
      await sendResponse(event, context, "FAILED", err);
    }
  }

  return;
};
