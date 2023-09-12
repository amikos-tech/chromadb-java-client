package tech.amikos.chromadb;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Embedding {


    private Collection collection;
    private List<List<Float>> embeddings = null;
    private List<Map<String, String>> metadatas = null;
    private List<String> documents = null;
    private List<String> ids = null;
    private Map<String,String> wheres;
    private Map<String,Object> whereDocuments;

    public Embedding(Collection collection){
        this.collection = collection;
    }


    public Embedding id(String id){
        if(this.ids == null)
            this.ids = new ArrayList<>();
        this.ids.add(id);
        return this;
    }

    public Embedding metadata(String key,String value){
        if(this.metadatas == null)
            metadatas = new ArrayList<>();
        if(this.metadatas.get(0) == null)
            metadatas.add(new HashMap<>());
        metadatas.get(metadatas.size()-1).put(key,value);
        return this;
    }

    public Embedding document(String document){
        if(this.documents == null)
            this.documents =  new ArrayList<>();
        documents.add(document);
        return this;
    }

    public Embedding embedding(Float embedding){
        if(this.embeddings == null)
            this.embeddings = new ArrayList<>();
        if(this.embeddings.get(0) == null)
            this.embeddings.add(new ArrayList<>());
        this.embeddings.get(embeddings.size()-1).add(embedding);
        return this;
    }

    public Object add(){
        return this.collection.add(embeddings,metadatas,documents,ids);
    }

    public Object batchAdd(){
        return this.collection.add(embeddings,metadatas,documents,ids);
    }

    public Embedding where(String key,String value){
        if(this.wheres == null)
            wheres = new HashMap<>();
        wheres.put(key,value);
        return this;
    }

    public Embedding whereDocument(String key,Object value){
        if(this.whereDocuments == null)
            whereDocuments = new HashMap<>();
        whereDocuments.put(key,value);
        return this;
    }

    public Collection.GetResult get(){
        if(ids == null)
            return this.collection.get();
        return this.collection.get(ids,wheres,whereDocuments);
    }

    public Object upsert(){
        return this.collection.upsert(embeddings,metadatas,documents,ids);
    }

    public Object update(){
        return this.collection.updateEmbeddings(embeddings,metadatas,documents,ids);
    }

    public Object delete(){
        return this.collection.delete(ids,wheres,whereDocuments);
    }


}
