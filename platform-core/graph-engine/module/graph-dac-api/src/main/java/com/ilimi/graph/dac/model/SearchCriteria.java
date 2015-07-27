package com.ilimi.graph.dac.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

import com.ilimi.graph.dac.enums.SystemProperties;

public class SearchCriteria implements Serializable {

    private static final long serialVersionUID = -6991536066924072138L;
    private String nodeType;
    private String objectType;
    private String op;
    private List<MetadataCriterion> metadata;
    private List<RelationCriterion> relations;
    private TagCriterion tag;
    private boolean countQuery;
    private int resultSize = 0;
    private int startPosition = 0;
    private List<String> fields = new LinkedList<String>();
    private List<Sort> sortOrder = new LinkedList<Sort>();


    Map<String, Object> params = new HashMap<String, Object>();
    int pIndex = 1;
    int index = 1;
    
    public Map<String, Object> getParams() {
        return this.params;
    }

    public List<MetadataCriterion> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataCriterion> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(MetadataCriterion mc) {
        if (null == metadata)
            metadata = new ArrayList<MetadataCriterion>();
        metadata.add(mc);
    }

    public String getOp() {
        if (StringUtils.isBlank(this.op))
            this.op = SearchConditions.LOGICAL_AND;
        return op;
    }

    public void setOp(String op) {
        if (StringUtils.equalsIgnoreCase(SearchConditions.LOGICAL_OR, op))
            this.op = SearchConditions.LOGICAL_OR;
        else
            this.op = SearchConditions.LOGICAL_AND;
    }

    public boolean isCountQuery() {
        return countQuery;
    }

    public void setCountQuery(boolean countQuery) {
        this.countQuery = countQuery;
    }

    public int getResultSize() {
        return resultSize;
    }

    public void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public List<RelationCriterion> getRelations() {
        return relations;
    }

    public void setRelations(List<RelationCriterion> relations) {
        this.relations = relations;
    }

    public void addRelationCriterion(RelationCriterion rc) {
        if (null == relations)
            relations = new ArrayList<RelationCriterion>();
        relations.add(rc);
    }

    public TagCriterion getTag() {
        return tag;
    }

    public void setTag(TagCriterion tag) {
        this.tag = tag;
    }

    @JsonIgnore
    public String getQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("MATCH (n:NODE) ");
        if (StringUtils.isNotBlank(nodeType) || StringUtils.isNotBlank(objectType) || (null != metadata && metadata.size() > 0)) {
            sb.append("WHERE ( ");
            if (StringUtils.isNotBlank(nodeType)) {
                sb.append(" n.").append(SystemProperties.IL_SYS_NODE_TYPE.name()).append(" = {").append(pIndex).append("} ");
                params.put("" + pIndex, nodeType);
                pIndex += 1;
            }
            if (StringUtils.isNotBlank(objectType)) {
                if (pIndex > 1)
                    sb.append("AND ");
                sb.append(" n.").append(SystemProperties.IL_FUNC_OBJECT_TYPE.name()).append(" = {").append(pIndex).append("} ");
                params.put("" + pIndex, objectType);
                pIndex += 1;
            }
            if (null != metadata && metadata.size() > 0) {
                if (pIndex > 1)
                    sb.append("AND ");
                for (int i = 0; i < metadata.size(); i++) {
                    String metadataCypher = metadata.get(i).getCypher(this, "n");
                    if(StringUtils.isNotBlank(metadataCypher)) {
                        sb.append(metadataCypher);
                        if (i < metadata.size() - 1)
                            sb.append(" ").append(getOp()).append(" ");
                    }
                }
            }
            sb.append(") ");
        }
        if (null != tag)
            sb.append(tag.getCypher(this, "n"));
        if (null != relations && relations.size() > 0) {
            for (RelationCriterion rel : relations)
                sb.append(rel.getCypher(this, null));
        }
        if (!countQuery) {
            if (null == fields || fields.isEmpty()) {
                sb.append("RETURN n ");
            } else {
                sb.append("RETURN ");
                for (int i = 0; i < fields.size(); i++) {
                    sb.append("n.").append(fields.get(i)).append(" as ").append(fields.get(i)).append(" ");
                    if (i < fields.size() - 1)
                        sb.append(", ");
                }
            }
            if (null != sortOrder && sortOrder.size() > 0) {
                sb.append("ORDER BY ");
                for (Sort sort : sortOrder) {
                    sb.append("n.").append(sort.getSortField()).append(" ");
                    if (StringUtils.equals(Sort.SORT_DESC, sort.getSortOrder())) {
                        sb.append("DESC ");
                    }
                }
            }

            if (startPosition > 0) {
                sb.append("SKIP ").append(startPosition).append(" ");
            }
            if (resultSize > 0) {
                sb.append("LIMIT ").append(resultSize).append(" ");
            }
        } else {
            sb.append("RETURN count(n) ");
        }
        return sb.toString();
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<Sort> getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(List<Sort> sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public void sort(Sort sort) {
        if (null == sortOrder)
            sortOrder = new LinkedList<Sort>();
        sortOrder.add(sort);
    }
    
    public static void main(String[] args) {

        SearchCriteria sc = new SearchCriteria();
        sc.setNodeType("DATA_NODE");
        sc.setObjectType("AssessmentItem");

        MetadataCriterion mc1 = MetadataCriterion.create(Arrays.asList(new Filter("prop1", "value1"), new Filter("prop2", SearchConditions.OP_NOT_EQUAL, "value2")), SearchConditions.LOGICAL_OR);
        MetadataCriterion mc11 = MetadataCriterion.create(Arrays.asList(new Filter("prop5", "value5"), new Filter("prop6", SearchConditions.OP_NOT_EQUAL, "value6")), SearchConditions.LOGICAL_AND);
        mc1.addMetadata(mc11);
        sc.addMetadata(mc1);

        MetadataCriterion mc2 = MetadataCriterion.create(Arrays.asList(new Filter("prop3", "value3"), new Filter("prop4", SearchConditions.OP_IN, Arrays.asList("1","2","3","4"))), SearchConditions.LOGICAL_OR);
        sc.addMetadata(mc2);

        TagCriterion tag = new TagCriterion(Arrays.asList("tag1", "tag2"));
        sc.setTag(tag);
        
        RelationCriterion rc1 = new RelationCriterion("associatedTo", "Concept");
        MetadataCriterion rmc1 = MetadataCriterion.create(Arrays.asList(new Filter("identifier", "C1"), new Filter("cLevel", "Level1")),  SearchConditions.LOGICAL_OR);
        rc1.addMetadata(rmc1);
        
        RelationCriterion rc11 = new RelationCriterion("associatedTo", "Game");
        MetadataCriterion rmc11 = MetadataCriterion.create(Arrays.asList(new Filter("os", "Android"), new Filter("ver", "4.4")));
        rc11.addMetadata(rmc11);
        
        MetadataCriterion rmc12 = MetadataCriterion.create(Arrays.asList(new Filter("os", "ios")));
        rc11.addMetadata(rmc12);
        rc11.setOp(SearchConditions.LOGICAL_OR);
        
        sc.addRelationCriterion(rc1);

        System.out.println(sc.getQuery());
        System.out.println(sc.params);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            String str = mapper.writeValueAsString(sc);
            
            System.out.println(str);
            System.out.println();
           
            
            SearchCriteria sc1 = mapper.readValue(str, SearchCriteria.class);
            System.out.println(sc1.getQuery());
            System.out.println(sc1.params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    }


}
