## Example of antivirus scanner for s3 Objects  

### Summary

This project is based on:

- Akka Http as Rest Server 
(possible protection layer for clamd which has no auth currently. Also abstracts socket communication with
REST)
- ClamAV open source antivirus scanner 
- Ceph as S3 backend
- Kafka message broker  

### How it works 

- Kafka topic is read every 5 seconds for new events [message format](https://docs.aws.amazon.com/AmazonS3/latest/dev/notification-content-structure.html)
- Rest API endpoint /scan is triggered with POST request
- Scanner gets file from S3 Bucket
- S3 object is streamed to clamd (listen on localhost)
- if s3 object is clean it is moved from upload subfolder to root of bucket,
if not is moved to contained subfolder of the bucket
- while s3 object is moved to destination subfolder metadata is added to object with scan status


### How to run

#### Environment setup

- Start ceph docker using docker-compose in this repo
- Start kafka broker and create topic (name: create_events)
- Download or install clamav (clamd) and start it
- clone current project, review settings and start it by running `sbt run`

By default Akka http will try to connect to scan engine at `localhost:3010` (tcp socket) and kafka at
`localhost:9092`, topic `create_events` (every 5 seconds)

#### Test commands 

1. Create test infected file (https://en.wikipedia.org/wiki/EICAR_test_file)
2. Use AWS cli to copy file to s3 bucket

```
aws s3 --endpoint http://127.0.0.1:8010 cp infectedFile.txt s3://demobucket/upload/infectedFile.txt
```

3. Scan file using REST endpoint

```
curl -H "Content-Type: application/json" -X POST -d '{"bucket":"demobucket", "key":"infectedFile.txt"}' http://localhost:8080/scan

response:
{"response":"Scan finished: ETAG: 69630e4574ec6798239b091cda43dca0, scan status: infected"}
```

in clamd log (debug) following line should appear

```
instream(127.0.0.1@39496): Eicar-Test-Signature(a542b55d256028f180b186de09da3b77:5468733) FOUND
```

4. Check the status of scanned file

NOTE: User-defined metadata is a set of key-value pairs. Amazon S3 stores user-defined metadata keys in lowercase 
```
[root@linux1 ~]# curl -X GET http://localhost:8080/status/demobucket/contained/infectedFile.txt |jq
[
  {
    "key": "scandate",
    "value": "2019-02-12T11:22:06.846Z"
  },
  {
    "key": "scanresult",
    "value": "infected"
  }
]
```

or using aws cli:

```
[root@linux1 ~]# aws s3api --endpoint-url http://127.0.0.1:8010 head-object --bucket demobucket --key infectedFile.txt
{
    "AcceptRanges": "bytes",
    "ContentType": "application/octet-stream",
    "LastModified": "Tue, 12 Feb 2019 11:44:52 GMT",
    "ContentLength": 7067,
    "ETag": "\"59fee5fab08fb15c3c7aa737e24549d6\"",
    "Metadata": {
        "scanresult": "clean",
        "scandate": "2019-02-12T11:44:52.241Z"
    }
}
```