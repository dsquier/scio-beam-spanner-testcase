# Scio / Beam / Spanner Test Case#

A re-producable test case to surface Spanner errors when using Scio 0.5.2 + Beam 2.4.0 + SpannerIO.

## Overview

After upgrading Scio from 0.5.0 -> 0.5.2 and Apache Beam from 2.2.0 -> 2.4.0, a batch Dataflow job is erroring out, unable to complete.

Additional observations:

- Downgrading back to Scio 0.5.0 + Beam 2.2.0 solves the problem. The jobs completes without error.
- This repo is a stripped down version of the job that fails.
- This repo uses `input.json` to reproduce the error. This is a 200 single-line JSON records with sensitive data redacted and is provided via a public GCP bucket. (No need to stage data).
- See "Error Logs" below for specific error that occurs.

## Steps to reproduce error

1) Create an account on GCP

2) Identify (or create) a GCP Project to run test case

3) Ensure SBT 1.1.4 is installed and GCP credentials configured

4) Create Spanner tables (DDL below). Make sure to replace `<YOUR-PROJECT-ID>` with the GCP project from #2

```
gcloud spanner instances create sciobeamspannertestcase --project=<YOUR-PROJECT-ID>
gcloud spanner databases create testcase --instance=sciobeamspannertestcase --project=<YOUR-PROJECT-ID>
gcloud spanner databases ddl update testcase --instance=sciobeamspannertestcase --project=<YOUR-PROJECT-ID> --ddl="CREATE TABLE parent_table (col01 STRING(64) NOT NULL,col02 STRING(64) NOT NULL,col03 STRING(32) NOT NULL,col04 INT64 NOT NULL,col05 INT64 NOT NULL,col06 INT64 NOT NULL,col07 INT64 NOT NULL,col08 INT64 NOT NULL) PRIMARY KEY (col03, col02, col01);"
gcloud spanner databases ddl update testcase --instance=sciobeamspannertestcase --project=<YOUR-PROJECT-ID> --ddl="CREATE TABLE child_table (col01 INT64 NOT NULL, col02 STRING(64) NOT NULL, col03 STRING(64) NOT NULL, col04 STRING(32) NOT NULL, col05 STRING(MAX), col06 STRING(MAX), col07 INT64 NOT NULL, col08 STRING(64) NOT NULL, col09 STRING(MAX), col10 INT64 NOT NULL, col11 INT64 NOT NULL, col12 STRING(MAX)) PRIMARY KEY (col04, col03, col02, col01);"
```

5) Execute Job:

```
sbt "runMain com.joinhoney.sciobeamspannertestcase.Testcase --project=<YOUR-PROJECT-ID> --runner=DataflowRunner --region=us-central1 --jobName=sciobeamspannertestcase"
```

## Error Logs

```
java.lang.RuntimeException: org.apache.beam.sdk.util.UserCodeException: java.lang.ArrayIndexOutOfBoundsException: 97
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowsParDoFn$1.output(GroupAlsoByWindowsParDoFn.java:183)
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowFnRunner$1.outputWindowedValue(GroupAlsoByWindowFnRunner.java:101)
	at com.google.cloud.dataflow.worker.util.BatchGroupAlsoByWindowViaIteratorsFn.processElement(BatchGroupAlsoByWindowViaIteratorsFn.java:124)
	at com.google.cloud.dataflow.worker.util.BatchGroupAlsoByWindowViaIteratorsFn.processElement(BatchGroupAlsoByWindowViaIteratorsFn.java:53)
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowFnRunner.invokeProcessElement(GroupAlsoByWindowFnRunner.java:114)
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowFnRunner.processElement(GroupAlsoByWindowFnRunner.java:72)
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowsParDoFn.processElement(GroupAlsoByWindowsParDoFn.java:113)
	at com.google.cloud.dataflow.worker.util.common.worker.ParDoOperation.process(ParDoOperation.java:43)
	at com.google.cloud.dataflow.worker.util.common.worker.OutputReceiver.process(OutputReceiver.java:48)
	at com.google.cloud.dataflow.worker.util.common.worker.ReadOperation.runReadLoop(ReadOperation.java:200)
	at com.google.cloud.dataflow.worker.util.common.worker.ReadOperation.start(ReadOperation.java:158)
	at com.google.cloud.dataflow.worker.util.common.worker.MapTaskExecutor.execute(MapTaskExecutor.java:75)
	at com.google.cloud.dataflow.worker.BatchDataflowWorker.executeWork(BatchDataflowWorker.java:383)
	at com.google.cloud.dataflow.worker.BatchDataflowWorker.doWork(BatchDataflowWorker.java:355)
	at com.google.cloud.dataflow.worker.BatchDataflowWorker.getAndPerformWork(BatchDataflowWorker.java:286)
	at com.google.cloud.dataflow.worker.DataflowBatchWorkerHarness$WorkerThread.doWork(DataflowBatchWorkerHarness.java:134)
	at com.google.cloud.dataflow.worker.DataflowBatchWorkerHarness$WorkerThread.call(DataflowBatchWorkerHarness.java:114)
	at com.google.cloud.dataflow.worker.DataflowBatchWorkerHarness$WorkerThread.call(DataflowBatchWorkerHarness.java:101)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
Caused by: org.apache.beam.sdk.util.UserCodeException: java.lang.ArrayIndexOutOfBoundsException: 97
	at org.apache.beam.sdk.util.UserCodeException.wrap(UserCodeException.java:36)
	at org.apache.beam.sdk.io.gcp.spanner.SpannerIO$BatchFn$DoFnInvoker.invokeProcessElement(Unknown Source)
	at org.apache.beam.runners.core.SimpleDoFnRunner.invokeProcessElement(SimpleDoFnRunner.java:177)
	at org.apache.beam.runners.core.SimpleDoFnRunner.processElement(SimpleDoFnRunner.java:138)
	at com.google.cloud.dataflow.worker.SimpleParDoFn.processElement(SimpleParDoFn.java:323)
	at com.google.cloud.dataflow.worker.util.common.worker.ParDoOperation.process(ParDoOperation.java:43)
	at com.google.cloud.dataflow.worker.util.common.worker.OutputReceiver.process(OutputReceiver.java:48)
	at com.google.cloud.dataflow.worker.GroupAlsoByWindowsParDoFn$1.output(GroupAlsoByWindowsParDoFn.java:181)
	... 21 more
Caused by: java.lang.ArrayIndexOutOfBoundsException: 97
	at org.apache.beam.sdk.io.gcp.spanner.MutationGroupEncoder.decodeMutation(MutationGroupEncoder.java:276)
	at org.apache.beam.sdk.io.gcp.spanner.MutationGroupEncoder.decode(MutationGroupEncoder.java:267)
	at org.apache.beam.sdk.io.gcp.spanner.SpannerIO$BatchFn.processElement(SpannerIO.java:944)
```