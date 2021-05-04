# search-insights-demo

This is the sample application build in the blog series of [Search|hub](https://blog.searchhub.io/how-to-diy-site-search-analytics-made-easy). 

## Get database count
curl -s localhost:8080/insights/count

## Upload a CSV file
curl -s http://localhost:8080/csv/upload -F file=@/path_to_sample_application/sample_data.csv

## List all uploaded CSV files
curl -s http://localhost:8080/csv/uploads

## Get the content of a uploaded file
curl -s http://localhost:8080/csv/uploads/sample_data.csv

## Upload the schema file of the example data
curl -s http://localhost:8080/csv/upload -F file=@/path_to_sample_application/sample_data.schema

## Convert the CSV file to Parquet
curl -s -XPATCH http://localhost:8080/csv/convert/sample_data.csv

## Upload the parquet file to S3
curl -s -XPATCH http://localhost:8080/csv/s3/sample_data.parquet
