# Firehose Transformer

This lambda function is triggered by firehose when the configured buffer conditions are met.  Buffering is based on a timeout (seconds) and batch size (MB).  

The Firehose Transformer shrinks the overall tweet status to only the relevant fields shown in the cut_payload function of main.py.  Enrichment steps can be added to this transform as well.  Note that the final line of cut_payload appends a '\n' character to the record.  This enables Athena to recognize one record from another and produces rows delimited by the '\n' character.  

# Athena

Once the firehose transformer has run and produced records in the firehose s3 bucket, run the following query in Athena to create a table:
```
CREATE EXTERNAL TABLE tweet_table (
  created_at string,
  id bigint,
  text string,
  user_name string,
  user_screen_name string,
  source string,
  retweet_count bigint,
  favorite_count bigint,
  lang string,
  verified boolean
  )           
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
LOCATION 's3://<firehose-s3-bucket>/records/'
```

This will create an external table in the default database which you can then preview and query the table from within Athena.