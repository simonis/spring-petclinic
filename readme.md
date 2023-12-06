# Spring PetClinic for AWS Lambda

This is a modified version of the original [Spring PetClinic Sample Application](https://github.com/spring-projects/spring-petclinic)
which can be deployed as an [AWS Lambda function with a function URL](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html).

In order to achieve this, the following changes have been done:

- Add a new dependency on [`aws-serverless-java-container-springboot2`](https://github.com/awslabs/aws-serverless-java-container/wiki) to `pom.xml`:
    ```xml
    <dependency>
      <groupId>com.amazonaws.serverless</groupId>
      <artifactId>aws-serverless-java-container-springboot2</artifactId>
      <version>1.9.1</version>
    </dependency>
   ```
- Remove the `spring-boot-maven-plugin` and replace it by the `maven-shade-plugin`. Instead of building the usual fat jar this will instead build a shaded version of the Spring PetClinic jar and remove the tomcat server from it. When running in AWS Lambda we don't need a web server because Lambda will act as the server. We also have to build an exploded, shaded jar such that we can start the PetClinic application without the special `org.springframework.boot.loader.JarLauncher` which is required if classes have to be loaded from nested jars.
- Add a new [handler](./src/main/java/org/springframework/samples/petclinic/PetClinicLambdaHandler.java) which basically wraps our PetClinic application:
    ```java
    public class PetClinicLambdaHandler implements RequestHandler<HttpApiV2ProxyRequest, AwsProxyResponse> {

      private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

      static {
        try {
          handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(PetClinicApplication.class);
        } catch (ContainerInitializationException ex) {
          throw new RuntimeException("Unable to load spring boot application", ex);
        }
      }

      @Override
      public AwsProxyResponse handleRequest(HttpApiV2ProxyRequest input, Context context) {
        return handler.proxy(input, context);
      }
    }
    ```
    You can find more details on the `SpringBootLambdaContainerHandler` in the [AWS serverless java container Wiki](https://github.com/awslabs/aws-serverless-java-container/wiki/Quick-start---Spring-Boot2).

After these changes, `mvn clean package` will create `target/spring-petclinic-2.7.3.jar` which can be deployed as a AWS Lambda function (be sure to configure 5120mb of memory to get enough vCPUs, otherwise the Spring startup time might exceed the 10 seconds limit for the Lambda's Init phase).

If we configure a function URL for the Lambda (under `Configuration -> Function URL`) we will get a URL of the form `https://<url-id>.lambda-url.<region>.on.aws/` which we can open directly in the browser and see the common PetClinic starting page.

This is obviously not the most performant way of running PetCLinic (opening `https://<url-id>.lambda-url.<region>.on.aws/` alone will start 6 concurrent instances of our function in order to download all the different images and CSS files the browser request for the landing page) but it more or less works.

The only thing which doesn't seem to work correctly through the Lambda function URL is serving binary content. You'll notice that the images on the starting page are not displayed. This is because they are served in base64 encoded form which the browser doesn't correctly recognize. Please let me know if you know how this can be fixed :).

## Running locally with RIE/RIC

The modified version of Spring PetClinic can also be run locally with the help of the [AWS Lambda Runtime Interface Emulator](https://github.com/aws/aws-lambda-runtime-interface-emulator/tree/develop) (RIE) and the [AWS Lambda Java Runtime Interface Client](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-runtime-interface-client) (RIC):
```bash
$ aws-lambda-rie java -cp \
  aws-lambda-java-core-1.2.3.jar:aws-lambda-java-runtime-interface-client-2.4.1.jar:aws-lambda-java-serialization-1.1.2.jar:spring-petclinic-2.7.3.jar \
  com.amazonaws.services.lambda.runtime.api.client.AWSLambda \
  org.springframework.samples.petclinic.PetClinicLambdaHandler::handleRequest
```
From another terminal we can then use `curl` to access the PetClinic:
```bash
$ curl "http://localhost:8080/2015-03-31/functions/function/invocations" -d '\
       { "rawPath": "/", "requestContext": { "http": { "method": "GET" } } }'

{"statusCode":200,"headers":{...},...,"body":"<!DOCTYPE html>\n\n<html>...</html>","base64Encoded":false}
```
Notice that the [request and response payloads](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html#http-api-develop-integrations-lambda.proxy-format) for our Lambda enabled for Function URLs are in the [Amazon API Gateway payload format version 2.0](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html#http-api-develop-integrations-lambda.proxy-format).

## Running locally with SAM CLI

Another way to run the Lambda locally is with the help of the [Serverless Application Model (SAM) CLI](https://github.com/aws/aws-sam-cli). For this we have to create a [SAM template](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-specification-template-anatomy.html).

### Creating a SAM template

The header of our [template.yaml](./template.yaml) looks as follows:
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: >
  PetClinicLambdaHandler
  Sample SAM Template for a PetClinic LambdaHandler
```
Next we define section with [global settings](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-specification-template-anatomy-globals.html):
```yaml
Globals:
  Function:
    Timeout: 20
    MemorySize: 512
    Environment:
      Variables:
        JAVA_TOOL_OPTIONS: "-Xlog:coops*=trace -DPetClinicLambdaHandler.log=true"
```
The timeout of 20 seconds is important because our PetClinic application might take more than the default 5 seconds for start-up. Here we can also define environment variables like `JAVA_TOOL_OPTIONS` which will be injected into the Docker container which will run our Lambda function and where they will be picked up by the JVM.

Finally we define our Lambda function in a [resource block](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html):
```yaml
Resources:
  PetClinicHttpHandler:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: ./
      Handler: org.springframework.samples.petclinic.PetClinicLambdaHandler::handleRequest
      Runtime: java21
      Events:
        SpringBoot2PetStoreRoot:
          Type: HttpApi
          Properties:
            Path: /
            Method: GET
        SpringBoot2PetStore:
          Type: HttpApi
          Properties:
            Path: /{proxy+}
            Method: ANY
    Metadata:
      BuildMethod: makefile
```
We create an [`AWS::Serverless::Function`](https://docs.aws.amazon.com/de_de/serverless-application-model/latest/developerguide/sam-resource-function.html) with the name `PetClinicHttpHandler`. Because we do all this in the root directory of the default Spring PetClinic application we define the `CodeUri` as `./` (i.e. PetClinic's top level directory). The handler of our Lambda function is the method `org.springframework.samples.petclinic.PetClinicLambdaHandler::handleRequest()` which we've created before.

The SAM CLI has various plugins and conventions for building the Lambda function but here we just want to use PetClinic's default Maven build, so we define an extra [`Makefile`](./Makefile) which will be used if we define the `BuildMethod` as `makefile` in the `Metadata` section of our resource:
```Makefile
build-PetClinicHttpHandler:
	mkdir -p $(ARTIFACTS_DIR)/lib
	# Use 'cd ${PWD}' to build from the top-level project directory instead of building from the
	# current tmp-directory '$(shell pwd)' created by SAM (see https://github.com/aws/aws-sam-cli/issues/4571).
	bash -c "cd ${PWD} && mvn -Dmaven.compiler.debug=true clean spring-javaformat:apply package"
	bash -c "cd ${PWD} && cp ./target/spring-petclinic*.jar $(ARTIFACTS_DIR)/lib/"
```
The `Makfile` needs to define a target called `build-{unction-name}` (i.e. in our case `build-PetClinicHttpHandler`) and it has to copy the build artifacts to `$(ARTIFACTS_DIR)/libs` where they will be picked up from the SAM CLI (the SAM CLI also sets `ARTIFACTS_DIR` before starting the build). Besides that we can trigger the normal Maven build (`mvn clean package`) from the Makefile.

Finally we have to define [`Events`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-eventsource.html) which will be used by the SAM CLI in order to call our Lambda function. We will define [`HttpApi`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-httpapi.html) events in contrast to the more powerful but also more complex [`Api`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-api.html) events (see ["Choosing between REST APIs and HTTP APIs"](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html) for a comparison of the two).

First we define the `SpringBoot2PetStoreRoot` event for the root path (`/`) and `GET` HTTP requests:
```yaml
        SpringBoot2PetStoreRoot:
          Type: HttpApi
          Properties:
            Path: /
            Method: GET
```
and second we define the `SpringBoot2PetStore` event for all other paths (`/{proxy+}` is a catch-all pattern for all request paths and `ANY` a wildcard for all HTTP requests - see ["Set up a method request in API Gateway"](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-method-settings-method-request.html#api-gateway-proxy-resource) for more details):
```yaml
        SpringBoot2PetStore:
          Type: HttpApi
          Properties:
            Path: /{proxy+}
            Method: ANY
```

### Using SAM CLI

With this setup we can now build (i.e. *prepare*) our PetClinic Lambda function for SAM:
```console
$ sam build
Building codeuri: /Git/spring-petclinic runtime: java21 metadata: {'BuildMethod': 'makefile'} architecture: x86_64 functions: PetClinicHttpHandler
PetClinicHttpHandler: Running CustomMakeBuilder:CopySource
PetClinicHttpHandler: Running CustomMakeBuilder:MakeBuild
PetClinicHttpHandler: Current Artifacts Directory : /Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler
mkdir -p /Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler/lib
# Use 'cd /Git/spring-petclinic' to build from the top-level project directory instead of building from the
# current tmp-directory '/tmp/tmptq3d62fc' created by SAM (see https://github.com/aws/aws-sam-cli/issues/4571).
bash -c "cd /Git/spring-petclinic && mvn -Dmaven.compiler.debug=true clean spring-javaformat:apply package"
[INFO] Scanning for projects...
[INFO]
[INFO] ------------< org.springframework.samples:spring-petclinic >------------
[INFO] Building petclinic 2.7.3
...
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  32.828 s
[INFO] Finished at: 2023-12-04T20:20:46+01:00
[INFO] ------------------------------------------------------------------------
bash -c "cd /Git/spring-petclinic && cp ./target/spring-petclinic*.jar /Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler/lib/"

Build Succeeded

Built Artifacts  : .aws-sam/build
Built Template   : .aws-sam/build/template.yaml
```
This basically calls our makefile which builds PetClinic with Maven and copies the resulting jar file to the SAM build directory `.aws-sam/build/PetClinicHttpHandler/lib/`.

#### SAM CLI `local invoke`
We're now ready to to locally invoke our Lambda function with the SAM CLI:
```console
$ sam local invoke
Invoking org.springframework.samples.petclinic.PetClinicLambdaHandler::handleRequest (java21)
Local image is up-to-date
Using local image: public.ecr.aws/lambda/java:21-rapid-x86_64.

Mounting /Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler as /var/task:ro,delegated, inside runtime container
START RequestId: abb76bb6-3545-4bb8-9cb5-3c2f00aef794 Version: $LATEST
Picked up JAVA_TOOL_OPTIONS: -Xlog:coops*=trace -DPetClinicLambdaHandler.log=true
[0.013s][trace][gc,heap,coops] Trying to allocate at address 0x00000000e4c00000 heap of size 0x1b400000
[0.013s][debug][gc,heap,coops] Heap address: 0x00000000e4c00000, size: 436 MB, Compressed Oops mode: 32-bit
19:44:39.174 [main] INFO com.amazonaws.serverless.proxy.internal.LambdaContainerHandler - Starting Lambda Container Handler
2023-12-04 19:44:39.548  INFO 15 --- [           main] o.s.samples.petclinic.MyAppListener      : => MyAppListener::environmentPrepared()


              |\      _,,,--,,_
             /,`.-'`'   ._  \-;;,_
  _______ __|,4-  ) )_   .;.(__`'-'__     ___ __    _ ___ _______
 |       | '---''(_/._)-'(_\_)   |   |   |   |  |  | |   |       |
 |    _  |    ___|_     _|       |   |   |   |   |_| |   |       | __ _ _
 |   |_| |   |___  |   | |       |   |   |   |       |   |       | \ \ \ \
 |    ___|    ___| |   | |      _|   |___|   |  _    |   |      _|  \ \ \ \
 |   |   |   |___  |   | |     |_|       |   | | |   |   |     |_    ) ) ) )
 |___|   |_______| |___| |_______|_______|___|_|  |__|___|_______|  / / / /
 ==================================================================/_/_/_/

:: Built with Spring Boot :: 2.7.3
...
```
SAM spins up a Docker container from the image `public.ecr.aws/lambda/java:21-rapid-x86_64` and starts the selected language runtime (`java21` in our case). We can see that the JVM picks up the `JAVA_TOOL_OPTIONS` we've defined in our template and starts our PetClinic application. We can observe more details from a parallel shell:
```console
$ docker ps --format "table {{.Image}}\t{{.Command}}\t{{.Ports}}\t{{.Names}}"
IMAGE                                        COMMAND                  PORTS                      NAMES
public.ecr.aws/lambda/java:21-rapid-x86_64   "/var/rapid/aws-lamb…"   127.0.0.1:5122->8080/tcp   peaceful_cray

$ docker top `docker ps -q` -eo user,pid,command
USER                PID                 COMMAND
root                315830              /var/rapid/aws-lambda-rie --log-level error
root                315885              /var/lang/bin/java ... -classpath /var/runtime/lib/aws-lambda-java-core-1.2.3.jar:/var/runtime/lib/aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:/var/runtime/lib/aws-lambda-java-serialization-1.1.4.jar \
                                          com.amazonaws.services.lambda.runtime.api.client.AWSLambda \
                                          org.springframework.samples.petclinic.PetClinicLambdaHandler::handleRequest
```
As you can see, the sam CLI basically runs the same commands (RIE and RIC) in the Docker container that we've used when we ran our Lambda manually in the section [Running locally with RIE/RIC](#running-locally-with-rieric).

Unfortunately we get an error when our Lambda handler is executed:
```console
...
2023-12-04 20:34:06.728  INFO 18 --- [           main] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2023-12-04 20:34:06.789 ERROR 18 --- [           main] c.a.s.p.internal.LambdaContainerHandler  : Error while handling request

com.amazonaws.serverless.exceptions.InvalidRequestEventException: The incoming event is not a valid HTTP API v2 proxy request
	at com.amazonaws.serverless.proxy.internal.servlet.AwsHttpApiV2HttpServletRequestReader.readRequest(AwsHttpApiV2HttpServletRequestReader.java:30) ~[spring-petclinic-2.7.3.jar:2.7.3]
	at com.amazonaws.serverless.proxy.internal.servlet.AwsHttpApiV2HttpServletRequestReader.readRequest(AwsHttpApiV2HttpServletRequestReader.java:24) ~[spring-petclinic-2.7.3.jar:2.7.3]
	at com.amazonaws.serverless.proxy.internal.LambdaContainerHandler.proxy(LambdaContainerHandler.java:204) ~[spring-petclinic-2.7.3.jar:2.7.3]
	at org.springframework.samples.petclinic.PetClinicLambdaHandler.handleRequest(PetClinicLambdaHandler.java:38) ~[spring-petclinic-2.7.3.jar:2.7.3]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Unknown Source) ~[na:na]
	at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$PojoMethodRequestHandler.handleRequest(EventHandlerLoader.java:285) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
	at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$PojoHandlerAsStreamHandler.handleRequest(EventHandlerLoader.java:202) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
	at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$2.call(EventHandlerLoader.java:905) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
	at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.startRuntime(AWSLambda.java:245) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
	at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.startRuntime(AWSLambda.java:197) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
	at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.main(AWSLambda.java:187) ~[aws-lambda-java-runtime-interface-client-2.4.1-linux-x86_64.jar:2.4.1]
...
END RequestId: a53d29e4-f56a-46a9-8fa7-180afeaec1d7
REPORT RequestId: a53d29e4-f56a-46a9-8fa7-180afeaec1d7	Init Duration: 0.25 ms	Duration: 6345.42 ms	Billed Duration: 6346 ms	Memory Size: 512 MB	Max Memory Used: 512 MB
{"statusCode": 500, "multiValueHeaders": {"Content-Type": ["application/json"]}, "body": "{\"message\":\"Internal Server Error\"}", "base64Encoded": false}
```
The reason is that we haven't passed any data (i.e. `Event`) to our handler.

If we pass the same data to our handler like in the manual example, the output looks much better and we will get the same result like before:
```console
$ echo '{ "rawPath": "/", "requestContext": { "http": { "method": "GET" } } }' | sam local invoke -e -
...
2023-12-04 20:49:44.993  INFO 16 --- [           main] c.a.s.p.internal.LambdaContainerHandler  : 127.0.0.1 --  [04/12/2023:20:49:44Z] "GET / null" 200 2885 "-" "-" combined
END RequestId: d1b424cb-9e29-4829-aca4-5e09b3af4478
REPORT RequestId: d1b424cb-9e29-4829-aca4-5e09b3af4478	Init Duration: 0.11 ms	Duration: 6632.03 ms	Billed Duration: 6633 ms	Memory Size: 512 MB	Max Memory Used: 512 MB
{"statusCode": 200," headers": {...}, ..., "body": "<!DOCTYPE html>\n\n<html>...</html>", "base64Encoded": false}
```
However, with SAM CLI, we don't have to manually craft the AWS Gateway API payload data by hand. We can instead use the handy `sam local generate-event` command and only change the parts which are important for us:
```console
$ sam local generate-event apigateway http-api-proxy --method GET --path "/" | sam local invoke -e -
...
{"statusCode": 200," headers": {...}, ..., "body": "<!DOCTYPE html>\n\n<html>...</html>", "base64Encoded": false}
```

Notice that `sam local invoke` only does a single function invocation after which the RIE/RIC as well as the Docker container are shut down.

#### SAM CLI `local start-lambda` and `aws lambda invoke`

If we want to invoke our Lambda function repeatedly, we can use the command `sam local start-lambda`. This will start a Docker container with RIE/RIC as before but at the same time also a local endpoint that emulates the AWS Lambda service. The endpoint can be used to invoke the Lambda function using the AWS CLI or SDK.

```console
$ sam local start-lambda
2023-12-06 16:46:17 Attaching import module proxy for analyzing dynamic imports
Initializing the lambda functions containers.
Local image is up-to-date
Using local image: public.ecr.aws/lambda/java:21-rapid-x86_64.

Mounting /Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler as /var/task:ro,delegated, inside runtime container
Containers Initialization is done.
Starting the Local Lambda Service. You can now invoke your Lambda Functions defined in your template through the endpoint.
2023-12-06 16:46:21 WARNING: This is a development server. Do not use it in a production deployment. Use a production WSGI server instead.
 * Running on http://127.0.0.1:3001
2023-12-06 16:46:21 Press CTRL+C to quit
```
From another terminal we can now invoke our Lambda function with AWS CLI as follows:
```console
$ aws lambda invoke --function-name PetClinicHttpHandler \
                    --endpoint http://127.0.0.1:3001 \
                    --payload '{ "rawPath": "/", "requestContext": { "http": { "method": "GET" } } }' \
                    --cli-binary-format raw-in-base64-out \
                    response.json
```

The real crucial part here is to use the `--cli-binary-format raw-in-base64-out` command line option with thw AWS CLI, otherwise you'll get weird error messages in SAM CLI (e.g. "`UnicodeDecodeError: 'utf-8' codec can't decode byte 0xbd in position 0: invalid start byte`") or AWS CLI itself (e.g. "`Invalid base64: "{ "helloo" : "world" }"`"). It took me days to find this solution in the [Use ‘cli-binary-format’ flag with AWS CLI version 2](https://medium.com/cloud-recipes/use-cli-binary-format-flag-with-aws-cli-version-2-34d590479280) and the corresponding explanation in the [AWS CLI documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html#cli-config-cli_binary_format).
#### SAM CLI `local start-api`

If we don't want to hand-craft JSON-events and like to receive the Lamabdas return value as true HTTP responses, we can use the `sam local start-api` command. This will expose the functionality of our Lambda function locally through a HTTP API, much like AWS API Gateway or Lambda's Function URL functionality are doing this:
```console
$ sam local start-api
Initializing the lambda functions containers.
Local image is up-to-date
Using local image: public.ecr.aws/lambda/java:21-rapid-x86_64.

Mounting /priv/simonisv/Git/spring-petclinic/.aws-sam/build/PetClinicHttpHandler as /var/task:ro,delegated, inside runtime container
Containers Initialization is done.
Mounting PetClinicHttpHandler at http://127.0.0.1:3000/{proxy+} [DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT]
Mounting PetClinicHttpHandler at http://127.0.0.1:3000/ [GET]
You can now browse to the above endpoints to invoke your functions. You do not need to restart/reload SAM CLI while working on your functions, changes will be reflected instantly/automatically.
If you used sam build before running local commands, you will need to re-run sam build for the changes to be picked up. You only need to restart SAM CLI if you update your AWS SAM template
2023-12-06 20:08:30 WARNING: This is a development server. Do not use it in a production deployment. Use a production WSGI server instead.
 * Running on http://127.0.0.1:3000
2023-12-06 20:08:30 Press CTRL+C to quit
```
Notice how the endpoints are automatically generated from the corresponding event specifications in the [`template.yaml`](./template.yaml) file.

We can now use either `curl` or the browser to directly access our Spring Petclinc:
```console
$ curl http://127.0.0.1:3000
<!DOCTYPE html>

<html>
...
</html>
```

Everything works except that binary data is still served base64 encoded (like in the very first example where we used Lambda Function URLs), even if we supply the right content type:
```console
$ curl --ignore-content-length -H 'Content-Type: image/png'  http://localhost:3000/resources/images/pets.png
iVBORw0KGgoAAA...AAASUVORK5CYII=
```
Notice that we also have to supply the `--ignore-content-length` command line option to `curl` because otherwise the output might be truncated. The reason is because the `Content-length` header returned by our Lambda function will contain the length of the original, binary data which is usually smaller than the length of the corresponding base64 encoded content.

#### Local debugging

Local debugging is quite simple. If we run RIE/RIC manually (as demonstrated in section [Running locally with RIE/RIC](#running-locally-with-rieric)) we can simply add the corresponding debugger parameters (e.g. `-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n`) directly to the Java command line.

With the SAM CLI, this becomes even easier, because the three local `invoke`, `start-lambda` and `start-api` commands all support the `-d <port>` option which will inject the `-agentlib:jdwp...` parameter into the Docker container with the help of the `_JAVA_OPTIONS` environment variable and route the corresponding debugging `<port>` out of the container:
```console
$ sam local start-api -d 1234
Initializing the lambda functions containers.
...
Picked up _JAVA_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=*:1234...
```



#### Local profiling

# Spring PetClinic Sample Application [![Build Status](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml)

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/spring-projects/spring-petclinic)

## Understanding the Spring Petclinic application with a few diagrams
<a href="https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application">See the presentation here</a>

## Running petclinic locally
Petclinic is a [Spring Boot](https://spring.io/guides/gs/spring-boot) application built using [Maven](https://spring.io/guides/gs/maven/) or [Gradle](https://spring.io/guides/gs/gradle/). You can build a jar file and run it from the command line (it should work just as well with Java 11 or newer):


```
git clone https://github.com/spring-projects/spring-petclinic.git
cd spring-petclinic
./mvnw package
java -jar target/*.jar
```

You can then access petclinic here: http://localhost:8080/

<img width="1042" alt="petclinic-screenshot" src="https://cloud.githubusercontent.com/assets/838318/19727082/2aee6d6c-9b8e-11e6-81fe-e889a5ddfded.png">

Or you can run it from Maven directly using the Spring Boot Maven plugin. If you do this it will pick up changes that you make in the project immediately (changes to Java source files require a compile as well - most people use an IDE for this):

```
./mvnw spring-boot:run
```

> NOTE: Windows users should set `git config core.autocrlf true` to avoid format assertions failing the build (use `--global` to set that flag globally).

> NOTE: If you prefer to use Gradle, you can build the app using `./gradlew build` and look for the jar file in `build/libs`.

## Building a Container

There is no `Dockerfile` in this project. You can build a container image (if you have a docker daemon) using the Spring Boot build plugin:

```
./mvnw spring-boot:build-image
```

## In case you find a bug/suggested improvement for Spring Petclinic
Our issue tracker is available here: https://github.com/spring-projects/spring-petclinic/issues


## Database configuration

In its default configuration, Petclinic uses an in-memory database (H2) which
gets populated at startup with data. The h2 console is automatically exposed at `http://localhost:8080/h2-console`
and it is possible to inspect the content of the database using the `jdbc:h2:mem:testdb` url.

A similar setup is provided for MySQL and PostgreSQL in case a persistent database configuration is needed. Note that whenever the database type is changed, the app needs to be run with a different profile: `spring.profiles.active=mysql` for MySQL or `spring.profiles.active=postgres` for PostgreSQL.

You could start MySQL or PostgreSQL locally with whatever installer works for your OS, or with docker:

```
docker run -e MYSQL_USER=petclinic -e MYSQL_PASSWORD=petclinic -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:5.7.8
```

or

```
docker run -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -e POSTGRES_DB=petclinic -p 5432:5432 postgres:14.1
```

Further documentation is provided for [MySQL](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/db/mysql/petclinic_db_setup_mysql.txt)
and for [PostgreSQL](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/db/postgres/petclinic_db_setup_postgres.txt).

## Compiling the CSS

There is a `petclinic.css` in `src/main/resources/static/resources/css`. It was generated from the `petclinic.scss` source, combined with the [Bootstrap](https://getbootstrap.com/) library. If you make changes to the `scss`, or upgrade Bootstrap, you will need to re-compile the CSS resources using the Maven profile "css", i.e. `./mvnw package -P css`. There is no build profile for Gradle to compile the CSS.

## Working with Petclinic in your IDE

### Prerequisites
The following items should be installed in your system:
* Java 11 or newer (full JDK not a JRE).
* git command line tool (https://help.github.com/articles/set-up-git)
* Your preferred IDE
  * Eclipse with the m2e plugin. Note: when m2e is available, there is an m2 icon in `Help -> About` dialog. If m2e is
  not there, just follow the install process here: https://www.eclipse.org/m2e/
  * [Spring Tools Suite](https://spring.io/tools) (STS)
  * IntelliJ IDEA
  * [VS Code](https://code.visualstudio.com)

### Steps:

1) On the command line
    ```
    git clone https://github.com/spring-projects/spring-petclinic.git
    ```
2) Inside Eclipse or STS
    ```
    File -> Import -> Maven -> Existing Maven project
    ```

    Then either build on the command line `./mvnw generate-resources` or using the Eclipse launcher (right click on project and `Run As -> Maven install`) to generate the css. Run the application main method by right clicking on it and choosing `Run As -> Java Application`.

3) Inside IntelliJ IDEA
    In the main menu, choose `File -> Open` and select the Petclinic [pom.xml](pom.xml). Click on the `Open` button.

    CSS files are generated from the Maven build. You can either build them on the command line `./mvnw generate-resources` or right click on the `spring-petclinic` project then `Maven -> Generates sources and Update Folders`.

    A run configuration named `PetClinicApplication` should have been created for you if you're using a recent Ultimate version. Otherwise, run the application by right clicking on the `PetClinicApplication` main class and choosing `Run 'PetClinicApplication'`.

4) Navigate to Petclinic

    Visit [http://localhost:8080](http://localhost:8080) in your browser.


## Looking for something in particular?

|Spring Boot Configuration | Class or Java property files  |
|--------------------------|---|
|The Main Class | [PetClinicApplication](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java) |
|Properties Files | [application.properties](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources) |
|Caching | [CacheConfiguration](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java) |

## Interesting Spring Petclinic branches and forks

The Spring Petclinic "main" branch in the [spring-projects](https://github.com/spring-projects/spring-petclinic)
GitHub org is the "canonical" implementation, currently based on Spring Boot and Thymeleaf. There are
[quite a few forks](https://spring-petclinic.github.io/docs/forks.html) in a special GitHub org
[spring-petclinic](https://github.com/spring-petclinic). If you have a special interest in a different technology stack
that could be used to implement the Pet Clinic then please join the community there.


## Interaction with other open source projects

One of the best parts about working on the Spring Petclinic application is that we have the opportunity to work in direct contact with many Open Source projects. We found some bugs/suggested improvements on various topics such as Spring, Spring Data, Bean Validation and even Eclipse! In many cases, they've been fixed/implemented in just a few days.
Here is a list of them:

| Name | Issue |
|------|-------|
| Spring JDBC: simplify usage of NamedParameterJdbcTemplate | [SPR-10256](https://jira.springsource.org/browse/SPR-10256) and [SPR-10257](https://jira.springsource.org/browse/SPR-10257) |
| Bean Validation / Hibernate Validator: simplify Maven dependencies and backward compatibility |[HV-790](https://hibernate.atlassian.net/browse/HV-790) and [HV-792](https://hibernate.atlassian.net/browse/HV-792) |
| Spring Data: provide more flexibility when working with JPQL queries | [DATAJPA-292](https://jira.springsource.org/browse/DATAJPA-292) |


# Contributing

The [issue tracker](https://github.com/spring-projects/spring-petclinic/issues) is the preferred channel for bug reports, features requests and submitting pull requests.

For pull requests, editor preferences are available in the [editor config](.editorconfig) for easy use in common text editors. Read more and download plugins at <https://editorconfig.org>. If you have not previously done so, please fill out and submit the [Contributor License Agreement](https://cla.pivotal.io/sign/spring).

# License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).

[spring-petclinic]: https://github.com/spring-projects/spring-petclinic
[spring-framework-petclinic]: https://github.com/spring-petclinic/spring-framework-petclinic
[spring-petclinic-angularjs]: https://github.com/spring-petclinic/spring-petclinic-angularjs
[javaconfig branch]: https://github.com/spring-petclinic/spring-framework-petclinic/tree/javaconfig
[spring-petclinic-angular]: https://github.com/spring-petclinic/spring-petclinic-angular
[spring-petclinic-microservices]: https://github.com/spring-petclinic/spring-petclinic-microservices
[spring-petclinic-reactjs]: https://github.com/spring-petclinic/spring-petclinic-reactjs
[spring-petclinic-graphql]: https://github.com/spring-petclinic/spring-petclinic-graphql
[spring-petclinic-kotlin]: https://github.com/spring-petclinic/spring-petclinic-kotlin
[spring-petclinic-rest]: https://github.com/spring-petclinic/spring-petclinic-rest
