import duckdb

items = duckdb.read_csv("D:\\용량 큰 자료들\\아마존 리뷰 추천 시스템\\items.csv")

duckdb.sql("SELECT DISTINCT main_category FROM items").show()

