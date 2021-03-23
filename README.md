# broker-cmdline-client
Java-based command line client to access notification records from the OpenAIRE Broker public API.

Notification records are stored as newline-delimited JSON files, one per subscription.

The produced files can be used as input to the <a href="https://4science.github.io/oaire-eld/#/data-correction">Data correction service for DSpace7/DSpace-CRIS 7 developed by 4Science</a> in the context of the <a href="https://www.openaire.eu/open-innovation-in-openaire">OpenAIRE-Advance Open Innovation Call</a>.

Full documentation, github links and installation instructions of the Data correction service are available <a href="https://4science.github.io/oaire-eld">here</a>.

## Compilation requirements
- git client
- Java 1.8
- Maven 3.6.0 (or above)

## Compilation instructions

```
git clone https://github.com/openaire/broker-cmdline-client.git                                                                                                   
Cloning into 'broker-cmdline-client'...
remote: Enumerating objects: 5, done.
remote: Counting objects: 100% (5/5), done.
remote: Compressing objects: 100% (5/5), done.
remote: Total 5 (delta 0), reused 0 (delta 0), pack-reused 0
Unpacking objects: 100% (5/5), done.

cd broker-cmdline-client/
mvn package
```

The compiled binary will be available under the `target` project subdirectory.

In UNIX-like systems the client binary can be executed with

```
./broker-cmdline-client-2.3.4.RELEASE.jar 
```

In Windows systems the client binary can be executed with
```
java -jar ./broker-cmdline-client-2.3.4.RELEASE.jar 
```


## Client synopsis

``` 
===================================
OpenAIRE Broker - Public API Client
===================================

usage: ./broker-cmdline-client.jar [-bu <arg>] [-h] [-i] -o <arg> -u <arg> [-v] [-vv]
        [-z]
-bu,--baseurl <arg>   override of the default Broker Public Api baseUrl
-h,--help             help
-i                    interactive mode
-o,--output <arg>     the output directory (REQUIRED)
-u,--user <arg>       the email of the subscriber (REQUIRED)
-v                    verbose
-vv                   show debug logs
-z                    compress the output files in GZIP format

See http://api.openaire.eu/broker for further details.
```

## Example usage

```
./broker-cmdline-client-2.3.4.RELEASE.jar -u useremail@domain.eu -i -z  -o /tmp/broker 
```

The command above performs the following actions
- lists all the subscriptions associated with the given user email
- for each subscription id asks for configurmation in interactive mode (-i) and
- produces a gzip compressed JSON file (-z)
- stored in the given output path (-o)


