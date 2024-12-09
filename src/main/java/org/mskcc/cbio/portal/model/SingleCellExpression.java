package org.mskcc.cbio.portal.model;

public class SingleCellExpression {
    private int sampleId;
    private int geneticProfileId;
    private CanonicalGene gene;
    private String cellType;
    private String tissue;
    private String expressionValue;

    public SingleCellExpression(int sampleId, int geneticProfileId, CanonicalGene gene, String cellType, String tissue,
            String expressionValue) {
        this.sampleId = sampleId;
        this.geneticProfileId = geneticProfileId;
        this.gene = gene;
        this.cellType = cellType;
        this.tissue = tissue;
        this.expressionValue = expressionValue;
    }

    public int getSampleId() {
        return sampleId;
    }

    public void setSampleId(int sampleId) {
        this.sampleId = sampleId;
    }

    public int getGeneticProfileId() {
        return geneticProfileId;
    }

    public void setGeneticProfileId(int geneticProfileId) {
        this.geneticProfileId = geneticProfileId;
    }

    public CanonicalGene getGene() {
        return gene;
    }

    public void setGene(CanonicalGene gene) {
        this.gene = gene;
    }

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }

    public String getExpressionValue() {
        return expressionValue;
    }

    public void setExpressionValue(String expressionValue) {
        this.expressionValue = expressionValue;
    }

}
