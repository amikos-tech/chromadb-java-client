package tech.amikos.chromadb;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query {

    private Collection collection;
    private List<String> queryTexts;
    private Integer nResults;
    private Map<String,String> wheres;
    private Map<String,String> whereDocuments;
    private List<QueryEmbedding.IncludeEnum> includes;
    public Query(){
    }

    public Query queryText(String queryText){
        if(this.queryTexts == null)
            this.queryTexts = new ArrayList<>();
        this.queryTexts.add(queryText);
        return this;
    }

    public Query nResults(Integer nResults){
        this.nResults = nResults;
        return this;
    }

    public Query where(String key,String value){
        if(this.wheres == null)
            wheres = new HashMap<>();
        wheres.put(key,value);
        return this;
    }

    public Query whereDocument(String key,String value){
        if(this.whereDocuments == null)
            whereDocuments = new HashMap<>();
        whereDocuments.put(key,value);
        return this;
    }

    public Query includeDocuments(){
        if(this.includes == null)
            this.includes = new ArrayList<QueryEmbedding.IncludeEnum>();
        this.includes.add(QueryEmbedding.IncludeEnum.DOCUMENTS);
        return this;
    }

    public Query includeEmbeddings(){
        if(this.includes == null)
            this.includes = new ArrayList<QueryEmbedding.IncludeEnum>();
        this.includes.add(QueryEmbedding.IncludeEnum.EMBEDDINGS);
        return this;
    }

    public Query includeMetadatas(){
        if(this.includes == null)
            this.includes = new ArrayList<QueryEmbedding.IncludeEnum>();
        this.includes.add(QueryEmbedding.IncludeEnum.METADATAS);
        return this;
    }

    public Query includeDistances(){
        if(this.includes == null)
            this.includes = new ArrayList<QueryEmbedding.IncludeEnum>();
        this.includes.add(QueryEmbedding.IncludeEnum.DISTANCES);
        return this;
    }

    public Collection.QueryResponse query(){
        return this.collection.query(queryTexts,nResults,wheres,whereDocuments,includes);
    }
}
