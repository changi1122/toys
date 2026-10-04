import os
from langchain_postgres import PGVector
from langchain_huggingface import HuggingFaceEmbeddings

embeddings = HuggingFaceEmbeddings(
    model_name="dragonkue/multilingual-e5-small-ko-v2"
)

vector_store = PGVector(
    embeddings=embeddings,
    collection_name="documents",
    connection=os.getenv("PG_CONNECTION"),
    use_jsonb=True,
)

results = vector_store.similarity_search(
    "What is coordinate chunk analysis",
    k=3,
)

for doc in results:
    print(doc.page_content)
    print(doc.metadata)

print()
print()

retriever = vector_store.as_retriever(
    search_kwargs={"k": 3}
)

docs = retriever.invoke("What is coordinate chunk analysis")

for doc in results:
    print(doc.page_content)
    print(doc.metadata)